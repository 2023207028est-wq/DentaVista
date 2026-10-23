package com.dentavista.servicio;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Consulta;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_ConsultaRepository;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class JE_JE_SR_JA_ME_PacienteService {

    private final JE_JE_SR_JA_ME_PacienteRepository pacientes;
    private final JE_JE_SR_JA_ME_ConsultaRepository consultas;
    private final JE_JE_SR_JA_ME_PagoService pagos;
    private final com.dentavista.repositorio.JE_JE_SR_JA_ME_ItemPlanRepository items;
    private final com.dentavista.repositorio.JE_JE_SR_JA_ME_CitaRepository citas;
    private final com.dentavista.repositorio.JE_JE_SR_JA_ME_UsuarioRepository usuarios;
    private final JE_JE_SR_JA_ME_Validacion validacion;
    private final JE_JE_SR_JA_ME_SesionService sesion;

    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_Paciente> JE_JE_SR_JA_ME_buscar(String texto) {
        sesion.JE_JE_SR_JA_ME_exigirPersonal();
        return pacientes.JE_JE_SR_JA_ME_buscar(texto == null ? "" : texto.trim(), true);
    }

    /** Pacientes dados de baja (archivados). */
    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_Paciente> JE_JE_SR_JA_ME_archivados(String texto) {
        sesion.JE_JE_SR_JA_ME_exigirPersonal();
        return pacientes.JE_JE_SR_JA_ME_buscar(texto == null ? "" : texto.trim(), false);
    }

    /**
     * Pacientes que ya no necesitan nada: terminaron todo su plan, lo pagaron y no
     * tienen citas por venir. Son los candidatos a archivar.
     */
    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_Paciente> JE_JE_SR_JA_ME_paraDarDeAlta() {
        sesion.JE_JE_SR_JA_ME_exigirPersonal();
        java.util.Set<Long> conPendientes = new java.util.HashSet<>();
        items.JE_JE_SR_JA_ME_pendientesPorPaciente().forEach(f -> conPendientes.add(((Number) f[0]).longValue()));
        LocalDateTime ahora = LocalDateTime.now();
        java.util.Set<Long> conCita = new java.util.HashSet<>();
        citas.findByInicioBetweenOrderByInicioAsc(ahora, ahora.plusYears(2)).stream()
                .filter(c -> c.getEstado().JE_JE_SR_JA_ME_abierta())
                .forEach(c -> conCita.add(c.getPaciente().getId()));
        return pagos.JE_JE_SR_JA_ME_saldos().entrySet().stream()
                .filter(e -> e.getValue().signum() <= 0 && !conPendientes.contains(e.getKey()) && !conCita.contains(e.getKey()))
                .map(e -> pacientes.findById(e.getKey()).orElse(null))
                .filter(p -> p != null && p.isActivo())
                .sorted(java.util.Comparator.comparing(JE_JE_SR_JA_ME_Paciente::getApellidos).thenComparing(JE_JE_SR_JA_ME_Paciente::getNombres))
                .toList();
    }

    @Transactional(readOnly = true)
    public JE_JE_SR_JA_ME_Paciente JE_JE_SR_JA_ME_obtener(Long id) {
        JE_JE_SR_JA_ME_Paciente p = pacientes.findById(id)
                .orElseThrow(() -> new JE_JE_SR_JA_ME_NegocioException("El paciente ya no existe."));
        sesion.JE_JE_SR_JA_ME_exigirAccesoA(p);
        return p;
    }

    public JE_JE_SR_JA_ME_Paciente JE_JE_SR_JA_ME_guardar(JE_JE_SR_JA_ME_Paciente p) {
        if (sesion.JE_JE_SR_JA_ME_haySesion()) {
            sesion.JE_JE_SR_JA_ME_exigirPersonal();
        }
        p.setDni(p.getDni() == null ? null : p.getDni().trim());
        p.setNombres(JE_JE_SR_JA_ME_limpiar(p.getNombres()));
        p.setApellidos(JE_JE_SR_JA_ME_limpiar(p.getApellidos()));
        validacion.JE_JE_SR_JA_ME_validar(p);
        pacientes.findByDni(p.getDni())
                .filter(otro -> !otro.getId().equals(p.getId()))
                .ifPresent(otro -> {
                    throw new JE_JE_SR_JA_ME_NegocioException("Ya existe un paciente con DNI " + p.getDni()
                            + ": " + otro.JE_JE_SR_JA_ME_nombreCompleto() + ".");
                });
        return pacientes.save(p);
    }

    /**
     * Da de baja a un paciente que ya no necesita atención. Es una baja lógica: su
     * historia clínica se conserva (obligación legal) y puede reactivarse. Se cancelan
     * sus citas por venir y se desactiva su acceso. No se archiva a quien aún debe.
     */
    public JE_JE_SR_JA_ME_Paciente JE_JE_SR_JA_ME_archivar(JE_JE_SR_JA_ME_Paciente p) {
        sesion.JE_JE_SR_JA_ME_exigirPersonal();
        JE_JE_SR_JA_ME_Paciente actual = JE_JE_SR_JA_ME_obtener(p.getId());
        java.math.BigDecimal deuda = pagos.JE_JE_SR_JA_ME_cuenta(actual).JE_JE_SR_JA_ME_deuda();
        if (deuda.signum() > 0) {
            throw new JE_JE_SR_JA_ME_NegocioException(actual.JE_JE_SR_JA_ME_nombreCompleto() + " debe S/ " + deuda.setScale(2, java.math.RoundingMode.HALF_UP)
                    + " por tratamientos realizados. Registra el pago antes de archivarlo.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        citas.findByPacienteOrderByInicioDesc(actual).stream()
                .filter(c -> c.getEstado().JE_JE_SR_JA_ME_abierta() && c.getInicio().isAfter(ahora))
                .forEach(c -> {
                    c.setEstado(com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita.CANCELADA);
                    citas.save(c);
                });
        usuarios.findByPaciente(actual).ifPresent(u -> {
            u.setActivo(false);
            usuarios.save(u);
        });
        actual.setActivo(false);
        return pacientes.save(actual);
    }

    /** Vuelve a activar a un paciente archivado (y su acceso, si tenía). */
    public JE_JE_SR_JA_ME_Paciente JE_JE_SR_JA_ME_reactivar(JE_JE_SR_JA_ME_Paciente p) {
        sesion.JE_JE_SR_JA_ME_exigirPersonal();
        JE_JE_SR_JA_ME_Paciente actual = JE_JE_SR_JA_ME_obtener(p.getId());
        usuarios.findByPaciente(actual).ifPresent(u -> {
            u.setActivo(true);
            usuarios.save(u);
        });
        actual.setActivo(true);
        return pacientes.save(actual);
    }

    @Transactional(readOnly = true)
    public Optional<LocalDateTime> JE_JE_SR_JA_ME_ultimaVisita(JE_JE_SR_JA_ME_Paciente p) {
        return consultas.findFirstByPacienteOrderByFechaDesc(p).map(JE_JE_SR_JA_ME_Consulta::getFecha);
    }

    private static String JE_JE_SR_JA_ME_limpiar(String s) {
        return s == null ? null : s.trim().replaceAll("\\s+", " ");
    }
}
