package com.dentavista.servicio;

import com.dentavista.dominio.JE_JE_SR_JA_ME_Medicamento;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Receta;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_RecetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Recetas: el odontólogo prescribe y el sistema cruza cada medicamento con las
 * alergias y antecedentes del paciente antes de emitirla.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class JE_JE_SR_JA_ME_RecetaService {

    private final JE_JE_SR_JA_ME_RecetaRepository recetas;
    private final JE_JE_SR_JA_ME_ClinicaService clinica;
    private final JE_JE_SR_JA_ME_SesionService sesion;

    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_Receta> JE_JE_SR_JA_ME_recetas(JE_JE_SR_JA_ME_Paciente p) {
        sesion.JE_JE_SR_JA_ME_exigirAccesoA(p);
        return recetas.findByPacienteOrderByFechaDesc(p);
    }

    /** Todas las alertas de la receta para este paciente. */
    public List<String> JE_JE_SR_JA_ME_alertas(JE_JE_SR_JA_ME_Paciente p, List<JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item> items) {
        List<String> r = new ArrayList<>();
        for (JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item i : items) {
            r.addAll(JE_JE_SR_JA_ME_Medicamento.JE_JE_SR_JA_ME_alertas(i.getMedicamento(), p.getAlergias(), p.getAntecedentes()));
        }
        return r;
    }

    /**
     * Emite la receta en la consulta de hoy.
     *
     * @param aceptarAlertas el odontólogo confirmó que vio las alertas (si las hay)
     */
    public JE_JE_SR_JA_ME_Receta JE_JE_SR_JA_ME_emitir(JE_JE_SR_JA_ME_Paciente p, List<JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item> items, String indicaciones, boolean aceptarAlertas) {
        sesion.JE_JE_SR_JA_ME_exigirClinica();
        List<JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item> limpios = items.stream()
                .filter(i -> i.getMedicamento() != null && !i.getMedicamento().isBlank())
                .map(i -> new JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item(i.getMedicamento().trim(),
                        i.getIndicacion() == null || i.getIndicacion().isBlank() ? "Según indicación" : i.getIndicacion().trim()))
                .toList();
        if (limpios.isEmpty()) {
            throw new JE_JE_SR_JA_ME_NegocioException("Agrega al menos un medicamento.");
        }
        List<String> alertas = JE_JE_SR_JA_ME_alertas(p, limpios);
        if (!alertas.isEmpty() && !aceptarAlertas) {
            throw new JE_JE_SR_JA_ME_NegocioException(String.join("\n", alertas));
        }
        return recetas.save(JE_JE_SR_JA_ME_Receta.builder()
                .paciente(p)
                .odontologo(sesion.JE_JE_SR_JA_ME_getUsuario())
                .consulta(clinica.JE_JE_SR_JA_ME_consultaDeHoy(p))
                .fecha(LocalDateTime.now())
                .items(new ArrayList<>(limpios))
                .indicaciones(indicaciones == null || indicaciones.isBlank() ? null : indicaciones.trim())
                .alertasAceptadas(alertas.isEmpty() ? null : String.join(" | ", alertas))
                .build());
    }

    /** Uso interno (datos de demostración). */
    public JE_JE_SR_JA_ME_Receta JE_JE_SR_JA_ME_guardar(JE_JE_SR_JA_ME_Receta r) {
        return recetas.save(r);
    }
}
