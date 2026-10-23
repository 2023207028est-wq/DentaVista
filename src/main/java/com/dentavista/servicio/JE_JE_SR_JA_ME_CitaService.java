package com.dentavista.servicio;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Cita;
import com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita;
import com.dentavista.modelo.JE_JE_SR_JA_ME_ItemPlan;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Rol;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Usuario;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_CitaRepository;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Agenda del consultorio.
 * <ul>
 *   <li>Horario de atención: lunes a viernes 8:00–13:00 y 15:00–19:00; sábados 8:00–13:00.</li>
 *   <li>Un odontólogo no puede tener dos citas a la vez, ni un paciente dos citas que se crucen.</li>
 *   <li>El <b>personal</b> agenda citas confirmadas; el <b>paciente</b> las solicita y la
 *       recepción las confirma.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class JE_JE_SR_JA_ME_CitaService {

    public static final int MINUTOS_BLOQUE = 30;
    private static final LocalTime[][] TURNOS_SEMANA = {
            {LocalTime.of(8, 0), LocalTime.of(13, 0)}, {LocalTime.of(15, 0), LocalTime.of(19, 0)}};
    private static final LocalTime[][] TURNOS_SABADO = {{LocalTime.of(8, 0), LocalTime.of(13, 0)}};

    private final JE_JE_SR_JA_ME_CitaRepository citas;
    private final JE_JE_SR_JA_ME_UsuarioRepository usuarios;
    private final com.dentavista.repositorio.JE_JE_SR_JA_ME_ItemPlanRepository items;
    private final com.dentavista.repositorio.JE_JE_SR_JA_ME_PacienteRepository pacientesRepo;
    private final JE_JE_SR_JA_ME_SesionService sesion;

    // --------------------------------------------------------------- lectura

    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_Usuario> JE_JE_SR_JA_ME_odontologos() {
        return usuarios.findByRolAndActivoTrueOrderByNombreAsc(JE_JE_SR_JA_ME_Rol.ODONTOLOGO);
    }

    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_Cita> JE_JE_SR_JA_ME_entre(LocalDate desde, LocalDate hastaInclusive) {
        sesion.JE_JE_SR_JA_ME_exigirPersonal();
        return citas.findByInicioBetweenOrderByInicioAsc(desde.atStartOfDay(), hastaInclusive.plusDays(1).atStartOfDay().minusNanos(1));
    }

    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_Cita> JE_JE_SR_JA_ME_delDia(LocalDate dia) {
        return JE_JE_SR_JA_ME_entre(dia, dia);
    }

    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_Cita> JE_JE_SR_JA_ME_dePaciente(JE_JE_SR_JA_ME_Paciente p) {
        sesion.JE_JE_SR_JA_ME_exigirAccesoA(p);
        return citas.findByPacienteOrderByInicioDesc(p);
    }

    @Transactional(readOnly = true)
    public Optional<JE_JE_SR_JA_ME_Cita> JE_JE_SR_JA_ME_proxima(JE_JE_SR_JA_ME_Paciente p) {
        sesion.JE_JE_SR_JA_ME_exigirAccesoA(p);
        LocalDateTime ahora = LocalDateTime.now();
        return citas.findByPacienteOrderByInicioDesc(p).stream()
                .filter(c -> c.getEstado().JE_JE_SR_JA_ME_abierta() && c.getInicio().isAfter(ahora))
                .reduce((a, b) -> b);
    }

    /** Paciente con tratamientos pendientes y sin cita próxima (hay que llamarlo). */
    public record JE_JE_SR_JA_ME_PorAgendar(JE_JE_SR_JA_ME_Paciente paciente, long tratamientos, java.math.BigDecimal monto) {
    }

    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_PorAgendar> JE_JE_SR_JA_ME_pacientesPorAgendar() {
        sesion.JE_JE_SR_JA_ME_exigirPersonal();
        LocalDateTime ahora = LocalDateTime.now();
        java.util.Set<Long> conCita = new java.util.HashSet<>();
        citas.findByInicioBetweenOrderByInicioAsc(ahora, ahora.plusYears(1)).stream()
                .filter(c -> c.getEstado().JE_JE_SR_JA_ME_abierta())
                .forEach(c -> conCita.add(c.getPaciente().getId()));
        List<JE_JE_SR_JA_ME_PorAgendar> r = new ArrayList<>();
        for (Object[] f : items.JE_JE_SR_JA_ME_pendientesPorPaciente()) {
            long id = ((Number) f[0]).longValue();
            if (!conCita.contains(id)) {
                pacientesRepo.findById(id).filter(JE_JE_SR_JA_ME_Paciente::isActivo).ifPresent(p ->
                        r.add(new JE_JE_SR_JA_ME_PorAgendar(p, ((Number) f[1]).longValue(), (java.math.BigDecimal) f[2])));
            }
        }
        r.sort(java.util.Comparator.comparing(JE_JE_SR_JA_ME_PorAgendar::monto).reversed());
        return r;
    }

    /** Solicitudes de pacientes pendientes de confirmar, la más próxima primero. */
    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_Cita> JE_JE_SR_JA_ME_solicitudes() {
        sesion.JE_JE_SR_JA_ME_exigirPersonal();
        LocalDateTime ahora = LocalDateTime.now();
        return citas.findByInicioBetweenOrderByInicioAsc(ahora, ahora.plusYears(1)).stream()
                .filter(c -> c.getEstado() == JE_JE_SR_JA_ME_EstadoCita.SOLICITADA).toList();
    }

    @Transactional(readOnly = true)
    public long JE_JE_SR_JA_ME_porConfirmar() {
        return citas.countByEstado(JE_JE_SR_JA_ME_EstadoCita.SOLICITADA);
    }

    /** Turnos de atención de un día (vacío si el consultorio no abre). */
    public static List<LocalTime[]> JE_JE_SR_JA_ME_turnos(LocalDate dia) {
        if (dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return List.of();
        }
        return List.of(dia.getDayOfWeek() == DayOfWeek.SATURDAY ? TURNOS_SABADO : TURNOS_SEMANA);
    }

    public static boolean JE_JE_SR_JA_ME_dentroDelHorario(LocalDateTime inicio, int duracion) {
        LocalTime i = inicio.toLocalTime();
        LocalTime f = i.plusMinutes(duracion);
        return JE_JE_SR_JA_ME_turnos(inicio.toLocalDate()).stream()
                .anyMatch(t -> !i.isBefore(t[0]) && !f.isAfter(t[1]) && f.isAfter(i));
    }

    /** Horas de inicio libres para un odontólogo en un día y una duración. */
    @Transactional(readOnly = true)
    public List<LocalTime> JE_JE_SR_JA_ME_horariosLibres(JE_JE_SR_JA_ME_Usuario odontologo, LocalDate dia, int duracion) {
        return JE_JE_SR_JA_ME_horariosLibres(odontologo, dia, duracion, null);
    }

    /** Igual, pero sin contar la cita que se está reprogramando (su propio horario cuenta como libre). */
    @Transactional(readOnly = true)
    public List<LocalTime> JE_JE_SR_JA_ME_horariosLibres(JE_JE_SR_JA_ME_Usuario odontologo, LocalDate dia, int duracion, JE_JE_SR_JA_ME_Cita excluida) {
        List<JE_JE_SR_JA_ME_Cita> ocupadas = citas.findByOdontologoAndInicioBetweenOrderByInicioAsc(odontologo,
                        dia.atStartOfDay(), dia.plusDays(1).atStartOfDay()).stream()
                .filter(c -> c.getEstado().JE_JE_SR_JA_ME_ocupaHorario() && !JE_JE_SR_JA_ME_esLaMisma(c, excluida)).toList();
        LocalDateTime minimo = LocalDateTime.now().plusMinutes(30);
        List<LocalTime> libres = new ArrayList<>();
        for (LocalTime[] t : JE_JE_SR_JA_ME_turnos(dia)) {
            for (LocalTime h = t[0]; !h.plusMinutes(duracion).isAfter(t[1]); h = h.plusMinutes(MINUTOS_BLOQUE)) {
                LocalDateTime i = dia.atTime(h);
                LocalDateTime f = i.plusMinutes(duracion);
                if (i.isAfter(minimo) && ocupadas.stream().noneMatch(c -> c.JE_JE_SR_JA_ME_seCruzaCon(i, f))) {
                    libres.add(h);
                }
            }
        }
        return libres;
    }

    // ----------------------------------------------------------- operaciones

    /**
     * Agenda (personal) o solicita (paciente) una cita.
     */
    public JE_JE_SR_JA_ME_Cita JE_JE_SR_JA_ME_agendar(JE_JE_SR_JA_ME_Paciente p, JE_JE_SR_JA_ME_Usuario odontologo, LocalDateTime inicio, int duracion, String motivo, JE_JE_SR_JA_ME_ItemPlan item) {
        if (sesion.JE_JE_SR_JA_ME_esPaciente()) {
            sesion.JE_JE_SR_JA_ME_exigirAccesoA(p);
        } else {
            sesion.JE_JE_SR_JA_ME_exigirPersonal();
        }
        if (p == null) {
            throw new JE_JE_SR_JA_ME_NegocioException("Elige el paciente.");
        }
        JE_JE_SR_JA_ME_validarHorario(p, odontologo, inicio, duracion, null);
        return citas.save(JE_JE_SR_JA_ME_Cita.builder()
                .paciente(p)
                .odontologo(odontologo)
                .inicio(inicio)
                .duracion(duracion)
                .motivo(motivo == null || motivo.isBlank() ? null : motivo.trim())
                .itemPlan(item)
                .estado(sesion.JE_JE_SR_JA_ME_esPaciente() ? JE_JE_SR_JA_ME_EstadoCita.SOLICITADA : JE_JE_SR_JA_ME_EstadoCita.CONFIRMADA)
                .creadaPor(sesion.JE_JE_SR_JA_ME_nombre())
                .creadaEn(LocalDateTime.now())
                .build());
    }

    /**
     * Cambia fecha, hora, duración u odontólogo de una cita abierta. Si la reprograma el
     * paciente vuelve a quedar "por confirmar"; si lo hace el personal queda confirmada.
     */
    public JE_JE_SR_JA_ME_Cita JE_JE_SR_JA_ME_reprogramar(JE_JE_SR_JA_ME_Cita c, JE_JE_SR_JA_ME_Usuario odontologo, LocalDateTime inicio, int duracion) {
        JE_JE_SR_JA_ME_Cita actual = JE_JE_SR_JA_ME_obtener(c);
        JE_JE_SR_JA_ME_exigirPuedeModificar(actual);
        JE_JE_SR_JA_ME_validarHorario(actual.getPaciente(), odontologo, inicio, duracion, actual);
        actual.setOdontologo(odontologo);
        actual.setInicio(inicio);
        actual.setDuracion(duracion);
        actual.setEstado(sesion.JE_JE_SR_JA_ME_esPaciente() ? JE_JE_SR_JA_ME_EstadoCita.SOLICITADA : JE_JE_SR_JA_ME_EstadoCita.CONFIRMADA);
        return citas.save(actual);
    }

    /** Fecha futura, dentro del horario y sin cruces para el odontólogo ni para el paciente. */
    private void JE_JE_SR_JA_ME_validarHorario(JE_JE_SR_JA_ME_Paciente p, JE_JE_SR_JA_ME_Usuario odontologo, LocalDateTime inicio, int duracion, JE_JE_SR_JA_ME_Cita excluida) {
        if (odontologo == null) {
            throw new JE_JE_SR_JA_ME_NegocioException("Elige el odontólogo.");
        }
        if (inicio == null) {
            throw new JE_JE_SR_JA_ME_NegocioException("Elige la fecha y la hora de la cita.");
        }
        if (duracion != 30 && duracion != 60 && duracion != 90) {
            throw new JE_JE_SR_JA_ME_NegocioException("La duración debe ser 30, 60 o 90 minutos.");
        }
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new JE_JE_SR_JA_ME_NegocioException("No se puede agendar una cita en el pasado.");
        }
        if (!JE_JE_SR_JA_ME_dentroDelHorario(inicio, duracion)) {
            throw new JE_JE_SR_JA_ME_NegocioException("Fuera del horario de atención (lunes a viernes 8:00–13:00 y 15:00–19:00, "
                    + "sábados 8:00–13:00).");
        }
        LocalDateTime fin = inicio.plusMinutes(duracion);
        citas.findByOdontologoAndInicioBetweenOrderByInicioAsc(odontologo, inicio.toLocalDate().atStartOfDay(),
                        inicio.toLocalDate().plusDays(1).atStartOfDay()).stream()
                .filter(c -> c.getEstado().JE_JE_SR_JA_ME_ocupaHorario() && !JE_JE_SR_JA_ME_esLaMisma(c, excluida) && c.JE_JE_SR_JA_ME_seCruzaCon(inicio, fin))
                .findFirst()
                .ifPresent(c -> {
                    throw new JE_JE_SR_JA_ME_NegocioException(odontologo.getNombre() + " ya tiene una cita de " + c.getInicio().toLocalTime()
                            + " a " + c.JE_JE_SR_JA_ME_fin().toLocalTime() + ". Elige otro horario.");
                });
        citas.findByPacienteAndInicioBetween(p, inicio.toLocalDate().atStartOfDay(), inicio.toLocalDate().plusDays(1).atStartOfDay())
                .stream()
                .filter(c -> c.getEstado().JE_JE_SR_JA_ME_ocupaHorario() && !JE_JE_SR_JA_ME_esLaMisma(c, excluida) && c.JE_JE_SR_JA_ME_seCruzaCon(inicio, fin))
                .findFirst()
                .ifPresent(c -> {
                    throw new JE_JE_SR_JA_ME_NegocioException(p.JE_JE_SR_JA_ME_nombreCompleto() + " ya tiene una cita a esa hora ("
                            + c.getInicio().toLocalTime() + ").");
                });
    }

    private static boolean JE_JE_SR_JA_ME_esLaMisma(JE_JE_SR_JA_ME_Cita c, JE_JE_SR_JA_ME_Cita otra) {
        return otra != null && c.getId().equals(otra.getId());
    }

    /** El personal modifica cualquier cita abierta; el paciente, solo las suyas futuras. */
    private void JE_JE_SR_JA_ME_exigirPuedeModificar(JE_JE_SR_JA_ME_Cita actual) {
        if (sesion.JE_JE_SR_JA_ME_esPaciente()) {
            sesion.JE_JE_SR_JA_ME_exigirAccesoA(actual.getPaciente());
            if (actual.getInicio().isBefore(LocalDateTime.now())) {
                throw new JE_JE_SR_JA_ME_NegocioException("La cita ya pasó.");
            }
        } else {
            sesion.JE_JE_SR_JA_ME_exigirPersonal();
        }
        if (!actual.getEstado().JE_JE_SR_JA_ME_abierta()) {
            throw new JE_JE_SR_JA_ME_NegocioException("La cita ya está " + actual.getEstado().JE_JE_SR_JA_ME_getEtiqueta().toLowerCase() + ".");
        }
    }

    public JE_JE_SR_JA_ME_Cita JE_JE_SR_JA_ME_confirmar(JE_JE_SR_JA_ME_Cita c) {
        sesion.JE_JE_SR_JA_ME_exigirPersonal();
        JE_JE_SR_JA_ME_Cita actual = JE_JE_SR_JA_ME_obtener(c);
        if (actual.getEstado() != JE_JE_SR_JA_ME_EstadoCita.SOLICITADA) {
            throw new JE_JE_SR_JA_ME_NegocioException("Solo se confirman citas solicitadas por el paciente.");
        }
        actual.setEstado(JE_JE_SR_JA_ME_EstadoCita.CONFIRMADA);
        return citas.save(actual);
    }

    /** El personal cancela cualquier cita abierta; el paciente, solo las suyas futuras. */
    public JE_JE_SR_JA_ME_Cita JE_JE_SR_JA_ME_cancelar(JE_JE_SR_JA_ME_Cita c) {
        JE_JE_SR_JA_ME_Cita actual = JE_JE_SR_JA_ME_obtener(c);
        JE_JE_SR_JA_ME_exigirPuedeModificar(actual);
        actual.setEstado(JE_JE_SR_JA_ME_EstadoCita.CANCELADA);
        return citas.save(actual);
    }

    public JE_JE_SR_JA_ME_Cita JE_JE_SR_JA_ME_marcarAtendida(JE_JE_SR_JA_ME_Cita c) {
        sesion.JE_JE_SR_JA_ME_exigirPersonal();
        JE_JE_SR_JA_ME_Cita actual = JE_JE_SR_JA_ME_obtener(c);
        if (!actual.getEstado().JE_JE_SR_JA_ME_abierta()) {
            throw new JE_JE_SR_JA_ME_NegocioException("La cita ya está " + actual.getEstado().JE_JE_SR_JA_ME_getEtiqueta().toLowerCase() + ".");
        }
        actual.setEstado(JE_JE_SR_JA_ME_EstadoCita.ATENDIDA);
        return citas.save(actual);
    }

    public JE_JE_SR_JA_ME_Cita JE_JE_SR_JA_ME_marcarNoAsistio(JE_JE_SR_JA_ME_Cita c) {
        sesion.JE_JE_SR_JA_ME_exigirPersonal();
        JE_JE_SR_JA_ME_Cita actual = JE_JE_SR_JA_ME_obtener(c);
        if (!actual.getEstado().JE_JE_SR_JA_ME_abierta()) {
            throw new JE_JE_SR_JA_ME_NegocioException("La cita ya está " + actual.getEstado().JE_JE_SR_JA_ME_getEtiqueta().toLowerCase() + ".");
        }
        actual.setEstado(JE_JE_SR_JA_ME_EstadoCita.NO_ASISTIO);
        return citas.save(actual);
    }

    /** Uso interno (datos de demostración): guarda sin validar horario ni fecha. */
    public JE_JE_SR_JA_ME_Cita JE_JE_SR_JA_ME_guardar(JE_JE_SR_JA_ME_Cita c) {
        return citas.save(c);
    }

    private JE_JE_SR_JA_ME_Cita JE_JE_SR_JA_ME_obtener(JE_JE_SR_JA_ME_Cita c) {
        return citas.findById(c.getId()).orElseThrow(() -> new JE_JE_SR_JA_ME_NegocioException("La cita ya no existe."));
    }
}
