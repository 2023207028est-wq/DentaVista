package com.dentavista.servicio;

import com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Odontograma;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Pieza;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Consulta;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Hallazgo;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_ConsultaRepository;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_HallazgoRepository;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_ItemPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Historia clínica odontológica: consultas y hallazgos sobre el odontograma.
 * <p>
 * Cada clic en el odontograma (3D o 2D) termina en {@link #JE_JE_SR_JA_ME_registrar}: valida que
 * el hallazgo tenga sentido clínico, lo guarda en la consulta del día y devuelve el
 * registro. El odontograma nunca se guarda "pintado": se reconstruye con
 * {@link #JE_JE_SR_JA_ME_odontograma(JE_JE_SR_JA_ME_Paciente, LocalDateTime)}.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class JE_JE_SR_JA_ME_ClinicaService {

    private final JE_JE_SR_JA_ME_HallazgoRepository hallazgos;
    private final JE_JE_SR_JA_ME_ConsultaRepository consultas;
    private final JE_JE_SR_JA_ME_ItemPlanRepository items;
    private final JE_JE_SR_JA_ME_SesionService sesion;

    // --------------------------------------------------------------- lectura

    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_Hallazgo> JE_JE_SR_JA_ME_hallazgos(JE_JE_SR_JA_ME_Paciente p) {
        sesion.JE_JE_SR_JA_ME_exigirAccesoA(p);
        return hallazgos.findByPacienteOrderByFechaAscIdAsc(p);
    }

    /** Boca del paciente en una fecha (null = estado actual). */
    @Transactional(readOnly = true)
    public JE_JE_SR_JA_ME_Odontograma JE_JE_SR_JA_ME_odontograma(JE_JE_SR_JA_ME_Paciente p, LocalDateTime hasta) {
        return JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_reconstruir(JE_JE_SR_JA_ME_hallazgos(p).stream().map(JE_JE_SR_JA_ME_Hallazgo::JE_JE_SR_JA_ME_aRegistro).toList(), hasta);
    }

    @Transactional(readOnly = true)
    public JE_JE_SR_JA_ME_Odontograma JE_JE_SR_JA_ME_odontograma(JE_JE_SR_JA_ME_Paciente p) {
        return JE_JE_SR_JA_ME_odontograma(p, null);
    }

    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_Hallazgo> JE_JE_SR_JA_ME_historialPieza(JE_JE_SR_JA_ME_Paciente p, int fdi) {
        sesion.JE_JE_SR_JA_ME_exigirAccesoA(p);
        return hallazgos.findByPacienteAndPiezaOrderByFechaDescIdDesc(p, fdi);
    }

    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_Consulta> JE_JE_SR_JA_ME_consultas(JE_JE_SR_JA_ME_Paciente p) {
        sesion.JE_JE_SR_JA_ME_exigirAccesoA(p);
        return consultas.findByPacienteOrderByFechaDesc(p);
    }

    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_Consulta> JE_JE_SR_JA_ME_consultasCronologicas(JE_JE_SR_JA_ME_Paciente p) {
        sesion.JE_JE_SR_JA_ME_exigirAccesoA(p);
        return consultas.findByPacienteOrderByFechaAsc(p);
    }

    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_Hallazgo> JE_JE_SR_JA_ME_hallazgosDe(JE_JE_SR_JA_ME_Consulta c) {
        return hallazgos.findByConsultaOrderByIdAsc(c);
    }

    // ------------------------------------------------------------- consultas

    /** La consulta de hoy del paciente; se abre automáticamente con el primer registro. */
    public JE_JE_SR_JA_ME_Consulta JE_JE_SR_JA_ME_consultaDeHoy(JE_JE_SR_JA_ME_Paciente p) {
        LocalDate hoy = LocalDate.now();
        return consultas.findFirstByPacienteAndFechaBetweenOrderByFechaDesc(
                        p, hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay().minusNanos(1))
                .orElseGet(() -> consultas.save(JE_JE_SR_JA_ME_Consulta.builder()
                        .paciente(p)
                        .odontologo(sesion.JE_JE_SR_JA_ME_getUsuario())
                        .fecha(LocalDateTime.now())
                        .motivo("Consulta odontológica")
                        .build()));
    }

    // -------------------------------------------------------------- registro

    /**
     * Registra un hallazgo hecho por el usuario en la consulta de hoy.
     *
     * @return el hallazgo guardado, o null si no cambia nada (la cara ya tenía ese estado)
     */
    public JE_JE_SR_JA_ME_Hallazgo JE_JE_SR_JA_ME_registrar(JE_JE_SR_JA_ME_Paciente p, int pieza, JE_JE_SR_JA_ME_Superficie superficie, JE_JE_SR_JA_ME_EstadoDental estado, String nota) {
        sesion.JE_JE_SR_JA_ME_exigirClinica();
        JE_JE_SR_JA_ME_validar(p, pieza, superficie, estado);
        JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_EstadoPieza actual = JE_JE_SR_JA_ME_odontograma(p).JE_JE_SR_JA_ME_pieza(pieza);
        if (JE_JE_SR_JA_ME_sinCambio(actual, superficie, estado)) {
            return null;
        }
        return JE_JE_SR_JA_ME_registrarEn(p, JE_JE_SR_JA_ME_consultaDeHoy(p), LocalDateTime.now(), pieza, superficie, estado, nota);
    }

    /** Registro sin comprobaciones de interfaz (datos de demostración y tratamientos realizados). */
    public JE_JE_SR_JA_ME_Hallazgo JE_JE_SR_JA_ME_registrarEn(JE_JE_SR_JA_ME_Paciente p, JE_JE_SR_JA_ME_Consulta consulta, LocalDateTime fecha, int pieza,
                                JE_JE_SR_JA_ME_Superficie superficie, JE_JE_SR_JA_ME_EstadoDental estado, String nota) {
        return hallazgos.save(JE_JE_SR_JA_ME_Hallazgo.builder()
                .paciente(p)
                .consulta(consulta)
                .pieza(pieza)
                .superficie(superficie)
                .estado(estado)
                .nota(nota == null || nota.isBlank() ? null : nota.trim())
                .fecha(fecha)
                .registradoPor(consulta != null && consulta.getOdontologo() != null
                        ? consulta.getOdontologo().getNombre() : sesion.JE_JE_SR_JA_ME_nombre())
                .build());
    }

    /** Valida que el hallazgo tenga sentido clínico antes de guardarlo. */
    public void JE_JE_SR_JA_ME_validar(JE_JE_SR_JA_ME_Paciente p, int pieza, JE_JE_SR_JA_ME_Superficie superficie, JE_JE_SR_JA_ME_EstadoDental estado) {
        if (!JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_esValida(pieza)) {
            throw new JE_JE_SR_JA_ME_NegocioException("La pieza " + pieza + " no existe en la notación FDI.");
        }
        if (superficie == JE_JE_SR_JA_ME_Superficie.PIEZA && !estado.JE_JE_SR_JA_ME_seAplicaAPieza()) {
            throw new JE_JE_SR_JA_ME_NegocioException(estado.JE_JE_SR_JA_ME_getEtiqueta() + " se registra sobre una cara: "
                    + "haz clic en la cara afectada de la pieza " + pieza + ".");
        }
        if (superficie != JE_JE_SR_JA_ME_Superficie.PIEZA && !estado.JE_JE_SR_JA_ME_seAplicaACara()) {
            throw new JE_JE_SR_JA_ME_NegocioException(estado.JE_JE_SR_JA_ME_getEtiqueta() + " afecta a la pieza completa; "
                    + "se registrará en toda la pieza " + pieza + ".");
        }
        if (superficie != JE_JE_SR_JA_ME_Superficie.PIEZA) {
            JE_JE_SR_JA_ME_EstadoDental general = JE_JE_SR_JA_ME_odontograma(p).JE_JE_SR_JA_ME_pieza(pieza).JE_JE_SR_JA_ME_general();
            if (general != null && general.JE_JE_SR_JA_ME_anulaCaras()) {
                throw new JE_JE_SR_JA_ME_NegocioException("La pieza " + pieza + " tiene registrado \""
                        + general.JE_JE_SR_JA_ME_getEtiqueta().toLowerCase() + "\": no tiene caras naturales. "
                        + "Usa \"Sano\" sobre la pieza para restablecerla si fue un error.");
            }
        }
    }

    private static boolean JE_JE_SR_JA_ME_sinCambio(JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_EstadoPieza actual, JE_JE_SR_JA_ME_Superficie s, JE_JE_SR_JA_ME_EstadoDental e) {
        if (s == JE_JE_SR_JA_ME_Superficie.PIEZA) {
            return e == JE_JE_SR_JA_ME_EstadoDental.SANO ? actual.JE_JE_SR_JA_ME_sana() : actual.JE_JE_SR_JA_ME_general() == e;
        }
        JE_JE_SR_JA_ME_EstadoDental cara = actual.JE_JE_SR_JA_ME_cara(s);
        return e == JE_JE_SR_JA_ME_EstadoDental.SANO ? cara == null : cara == e;
    }

    /**
     * Deshace el último hallazgo registrado hoy para el paciente.
     *
     * @return el hallazgo eliminado, o null si hoy no hay nada que deshacer
     */
    public JE_JE_SR_JA_ME_Hallazgo JE_JE_SR_JA_ME_deshacerUltimo(JE_JE_SR_JA_ME_Paciente p) {
        sesion.JE_JE_SR_JA_ME_exigirClinica();
        LocalDate hoy = LocalDate.now();
        return consultas.findFirstByPacienteAndFechaBetweenOrderByFechaDesc(
                        p, hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay().minusNanos(1))
                .flatMap(c -> hallazgos.findFirstByPacienteAndConsultaOrderByIdDesc(p, c).map(h -> {
                    hallazgos.delete(h);
                    // Una consulta abierta solo por ese registro desaparece con él.
                    if (hallazgos.findByConsultaOrderByIdAsc(c).isEmpty() && !items.existsByConsulta(c)) {
                        consultas.delete(c);
                    }
                    return h;
                }))
                .orElse(null);
    }
}
