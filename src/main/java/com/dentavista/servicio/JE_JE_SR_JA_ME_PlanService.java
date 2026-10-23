package com.dentavista.servicio;

import com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Odontograma;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Pieza;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Consulta;
import com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoItem;
import com.dentavista.modelo.JE_JE_SR_JA_ME_ItemPlan;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Tratamiento;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_ItemPlanRepository;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_TratamientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Plan de tratamiento y presupuesto. Cierra el ciclo clínico:
 * <b>diagnóstico</b> (hallazgos) → <b>plan</b> (generado automáticamente) →
 * <b>presupuesto</b> (PDF) → <b>tratamiento realizado</b>, que a su vez actualiza
 * el odontograma (la caries pasa a obturación, la extracción deja la pieza ausente...).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class JE_JE_SR_JA_ME_PlanService {

    /** Totales del plan de un paciente. */
    public record JE_JE_SR_JA_ME_Totales(BigDecimal pendiente, BigDecimal realizado, int itemsPendientes, int itemsRealizados) {
        public BigDecimal JE_JE_SR_JA_ME_total() {
            return pendiente.add(realizado);
        }

        /** Avance del plan entre 0 y 1 (por importe). */
        public double JE_JE_SR_JA_ME_avance() {
            BigDecimal t = JE_JE_SR_JA_ME_total();
            return t.signum() == 0 ? 0 : realizado.doubleValue() / t.doubleValue();
        }
    }

    private final JE_JE_SR_JA_ME_ItemPlanRepository items;
    private final JE_JE_SR_JA_ME_TratamientoRepository tratamientos;
    private final JE_JE_SR_JA_ME_ClinicaService clinica;
    private final JE_JE_SR_JA_ME_SesionService sesion;

    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_ItemPlan> JE_JE_SR_JA_ME_items(JE_JE_SR_JA_ME_Paciente p) {
        sesion.JE_JE_SR_JA_ME_exigirAccesoA(p);
        return items.findByPacienteOrderByCreadoAscIdAsc(p);
    }

    @Transactional(readOnly = true)
    public JE_JE_SR_JA_ME_Totales JE_JE_SR_JA_ME_totales(JE_JE_SR_JA_ME_Paciente p) {
        BigDecimal pendiente = BigDecimal.ZERO;
        BigDecimal realizado = BigDecimal.ZERO;
        int np = 0;
        int nr = 0;
        for (JE_JE_SR_JA_ME_ItemPlan i : JE_JE_SR_JA_ME_items(p)) {
            if (i.getEstado() == JE_JE_SR_JA_ME_EstadoItem.PENDIENTE) {
                pendiente = pendiente.add(i.getPrecio());
                np++;
            } else if (i.getEstado() == JE_JE_SR_JA_ME_EstadoItem.REALIZADO) {
                realizado = realizado.add(i.getPrecio());
                nr++;
            }
        }
        return new JE_JE_SR_JA_ME_Totales(pendiente, realizado, np, nr);
    }

    /**
     * Agrega al plan un tratamiento por cada patología del odontograma que todavía
     * no tenga uno pendiente (caries → obturación, extracción indicada → exodoncia...).
     *
     * @return cuántos tratamientos se agregaron
     */
    public int JE_JE_SR_JA_ME_generarDesdeOdontograma(JE_JE_SR_JA_ME_Paciente p) {
        sesion.JE_JE_SR_JA_ME_exigirClinica();
        JE_JE_SR_JA_ME_Odontograma o = clinica.JE_JE_SR_JA_ME_odontograma(p);
        List<JE_JE_SR_JA_ME_ItemPlan> pendientes = items.findByPacienteAndEstado(p, JE_JE_SR_JA_ME_EstadoItem.PENDIENTE);
        int agregados = 0;
        for (JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_Patologia pat : o.JE_JE_SR_JA_ME_patologias()) {
            JE_JE_SR_JA_ME_Tratamiento t = tratamientos.findByCodigo(pat.estado().JE_JE_SR_JA_ME_getTratamientoSugerido())
                    .filter(JE_JE_SR_JA_ME_Tratamiento::isActivo)
                    .orElse(null);
            if (t == null) {
                continue;
            }
            JE_JE_SR_JA_ME_Superficie s = t.isPorCara() ? pat.superficie() : JE_JE_SR_JA_ME_Superficie.PIEZA;
            boolean yaEsta = pendientes.stream().anyMatch(i ->
                    i.getTratamiento().getId().equals(t.getId())
                            && Objects.equals(i.getPieza(), pat.pieza())
                            && i.getSuperficie() == s);
            if (!yaEsta) {
                pendientes.add(items.save(JE_JE_SR_JA_ME_nuevo(p, t, pat.pieza(), s, LocalDateTime.now(),
                        pat.estado().JE_JE_SR_JA_ME_getEtiqueta() + " en " + pat.superficie().JE_JE_SR_JA_ME_etiqueta(pat.pieza()).toLowerCase())));
                agregados++;
            }
        }
        return agregados;
    }

    /** Agrega un tratamiento manualmente. */
    public JE_JE_SR_JA_ME_ItemPlan JE_JE_SR_JA_ME_agregar(JE_JE_SR_JA_ME_Paciente p, JE_JE_SR_JA_ME_Tratamiento t, Integer pieza, JE_JE_SR_JA_ME_Superficie superficie) {
        sesion.JE_JE_SR_JA_ME_exigirClinica();
        if (t == null) {
            throw new JE_JE_SR_JA_ME_NegocioException("Elige un tratamiento del catálogo.");
        }
        if (t.isRequierePieza() && pieza == null) {
            throw new JE_JE_SR_JA_ME_NegocioException(t.getNombre() + " se realiza sobre una pieza: elige cuál.");
        }
        if (pieza != null && !JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_esValida(pieza)) {
            throw new JE_JE_SR_JA_ME_NegocioException("La pieza " + pieza + " no existe en la notación FDI.");
        }
        JE_JE_SR_JA_ME_Superficie s = superficie;
        if (!t.isRequierePieza()) {
            pieza = null;
            s = null;
        } else if (t.isPorCara()) {
            if (s == null || !s.JE_JE_SR_JA_ME_esCara()) {
                throw new JE_JE_SR_JA_ME_NegocioException(t.getNombre() + " se aplica por cara: elige la cara de la pieza " + pieza + ".");
            }
        } else {
            s = JE_JE_SR_JA_ME_Superficie.PIEZA;
        }
        return items.save(JE_JE_SR_JA_ME_nuevo(p, t, pieza, s, LocalDateTime.now(), null));
    }

    /**
     * Marca el tratamiento como realizado en la consulta de hoy y registra en el
     * odontograma el estado resultante.
     */
    public JE_JE_SR_JA_ME_ItemPlan JE_JE_SR_JA_ME_realizar(JE_JE_SR_JA_ME_ItemPlan item) {
        sesion.JE_JE_SR_JA_ME_exigirClinica();
        JE_JE_SR_JA_ME_ItemPlan i = items.findById(item.getId())
                .orElseThrow(() -> new JE_JE_SR_JA_ME_NegocioException("El tratamiento ya no existe."));
        if (i.getEstado() != JE_JE_SR_JA_ME_EstadoItem.PENDIENTE) {
            throw new JE_JE_SR_JA_ME_NegocioException("Solo se pueden realizar tratamientos pendientes.");
        }
        JE_JE_SR_JA_ME_Paciente p = i.getPaciente();
        JE_JE_SR_JA_ME_EstadoDental resultado = i.getTratamiento().getEstadoResultante();
        if (resultado != null && i.getPieza() != null) {
            JE_JE_SR_JA_ME_Superficie s = i.getSuperficie() != null && i.getSuperficie().JE_JE_SR_JA_ME_esCara() && resultado.JE_JE_SR_JA_ME_seAplicaACara()
                    ? i.getSuperficie() : JE_JE_SR_JA_ME_Superficie.PIEZA;
            clinica.JE_JE_SR_JA_ME_validar(p, i.getPieza(), s, resultado);
        }
        JE_JE_SR_JA_ME_Consulta hoy = clinica.JE_JE_SR_JA_ME_consultaDeHoy(p);
        LocalDateTime ahora = LocalDateTime.now();
        JE_JE_SR_JA_ME_aplicarRealizacion(i, hoy, ahora);
        return items.save(i);
    }

    /** Usado también por los datos de demostración para fechar tratamientos en el pasado. */
    public void JE_JE_SR_JA_ME_aplicarRealizacion(JE_JE_SR_JA_ME_ItemPlan i, JE_JE_SR_JA_ME_Consulta consulta, LocalDateTime fecha) {
        i.setEstado(JE_JE_SR_JA_ME_EstadoItem.REALIZADO);
        i.setRealizado(fecha);
        i.setConsulta(consulta);
        JE_JE_SR_JA_ME_EstadoDental resultado = i.getTratamiento().getEstadoResultante();
        if (resultado != null && i.getPieza() != null) {
            JE_JE_SR_JA_ME_Superficie s = i.getSuperficie() != null && i.getSuperficie().JE_JE_SR_JA_ME_esCara() && resultado.JE_JE_SR_JA_ME_seAplicaACara()
                    ? i.getSuperficie() : JE_JE_SR_JA_ME_Superficie.PIEZA;
            clinica.JE_JE_SR_JA_ME_registrarEn(i.getPaciente(), consulta, fecha, i.getPieza(), s, resultado,
                    i.getTratamiento().getNombre());
        }
        items.save(i);
    }

    public JE_JE_SR_JA_ME_ItemPlan JE_JE_SR_JA_ME_cancelar(JE_JE_SR_JA_ME_ItemPlan item) {
        sesion.JE_JE_SR_JA_ME_exigirClinica();
        JE_JE_SR_JA_ME_ItemPlan i = items.findById(item.getId())
                .orElseThrow(() -> new JE_JE_SR_JA_ME_NegocioException("El tratamiento ya no existe."));
        if (i.getEstado() != JE_JE_SR_JA_ME_EstadoItem.PENDIENTE) {
            throw new JE_JE_SR_JA_ME_NegocioException("Solo se pueden cancelar tratamientos pendientes.");
        }
        i.setEstado(JE_JE_SR_JA_ME_EstadoItem.CANCELADO);
        return items.save(i);
    }

    public JE_JE_SR_JA_ME_ItemPlan JE_JE_SR_JA_ME_reabrir(JE_JE_SR_JA_ME_ItemPlan item) {
        sesion.JE_JE_SR_JA_ME_exigirClinica();
        JE_JE_SR_JA_ME_ItemPlan i = items.findById(item.getId())
                .orElseThrow(() -> new JE_JE_SR_JA_ME_NegocioException("El tratamiento ya no existe."));
        if (i.getEstado() != JE_JE_SR_JA_ME_EstadoItem.CANCELADO) {
            throw new JE_JE_SR_JA_ME_NegocioException("Solo se pueden reabrir tratamientos cancelados.");
        }
        i.setEstado(JE_JE_SR_JA_ME_EstadoItem.PENDIENTE);
        return items.save(i);
    }

    public JE_JE_SR_JA_ME_ItemPlan JE_JE_SR_JA_ME_nuevo(JE_JE_SR_JA_ME_Paciente p, JE_JE_SR_JA_ME_Tratamiento t, Integer pieza, JE_JE_SR_JA_ME_Superficie s, LocalDateTime creado, String nota) {
        return JE_JE_SR_JA_ME_ItemPlan.builder()
                .paciente(p)
                .tratamiento(t)
                .pieza(pieza)
                .superficie(s)
                .precio(t.getPrecio())
                .estado(JE_JE_SR_JA_ME_EstadoItem.PENDIENTE)
                .creado(creado)
                .nota(nota)
                .build();
    }

    public JE_JE_SR_JA_ME_ItemPlan JE_JE_SR_JA_ME_guardar(JE_JE_SR_JA_ME_ItemPlan i) {
        return items.save(i);
    }
}
