package com.dentavista.servicio;

import com.dentavista.JE_JE_SR_JA_ME_IntegracionBase;
import com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoItem;
import com.dentavista.modelo.JE_JE_SR_JA_ME_ItemPlan;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental.*;
import static com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Ciclo completo: diagnóstico → plan → realizado → odontograma actualizado. */
class JE_JE_SR_JA_ME_PlanServiceTest extends JE_JE_SR_JA_ME_IntegracionBase {

    @Autowired
    private JE_JE_SR_JA_ME_ClinicaService clinica;
    @Autowired
    private JE_JE_SR_JA_ME_PlanService plan;

    private JE_JE_SR_JA_ME_Paciente paciente;

    @BeforeEach
    void JE_JE_SR_JA_ME_preparar() {
        JE_JE_SR_JA_ME_iniciarSesion(JE_JE_SR_JA_ME_Rol.ODONTOLOGO);
        paciente = JE_JE_SR_JA_ME_pacienteNuevo();
        JE_JE_SR_JA_ME_tratamiento("OBT", "90", OBTURADO, true);
        JE_JE_SR_JA_ME_tratamiento("EXO", "150", AUSENTE, false);
        JE_JE_SR_JA_ME_tratamiento("END", "480", ENDODONCIA, false);
    }

    @Test
    void JE_JE_SR_JA_ME_generaUnTratamientoPorCadaPatologiaSinDuplicar() {
        clinica.JE_JE_SR_JA_ME_registrar(paciente, 16, OCLUSAL, CARIES, null);
        clinica.JE_JE_SR_JA_ME_registrar(paciente, 16, MESIAL, CARIES, null);
        clinica.JE_JE_SR_JA_ME_registrar(paciente, 48, PIEZA, EXTRACCION_INDICADA, null);

        assertThat(plan.JE_JE_SR_JA_ME_generarDesdeOdontograma(paciente)).isEqualTo(3);
        assertThat(plan.JE_JE_SR_JA_ME_generarDesdeOdontograma(paciente)).isZero();

        List<JE_JE_SR_JA_ME_ItemPlan> items = plan.JE_JE_SR_JA_ME_items(paciente);
        assertThat(items).extracting(i -> i.getTratamiento().getCodigo())
                .containsExactlyInAnyOrder("OBT", "OBT", "EXO");
        assertThat(plan.JE_JE_SR_JA_ME_totales(paciente).pendiente()).isEqualByComparingTo(new BigDecimal("330"));
    }

    @Test
    void JE_JE_SR_JA_ME_realizarUnaObturacionCuraLaCariesEnElOdontograma() {
        clinica.JE_JE_SR_JA_ME_registrar(paciente, 26, OCLUSAL, CARIES, null);
        plan.JE_JE_SR_JA_ME_generarDesdeOdontograma(paciente);
        JE_JE_SR_JA_ME_ItemPlan obt = plan.JE_JE_SR_JA_ME_items(paciente).get(0);

        plan.JE_JE_SR_JA_ME_realizar(obt);

        assertThat(clinica.JE_JE_SR_JA_ME_odontograma(paciente).JE_JE_SR_JA_ME_pieza(26).JE_JE_SR_JA_ME_cara(OCLUSAL)).isEqualTo(OBTURADO);
        assertThat(clinica.JE_JE_SR_JA_ME_odontograma(paciente).JE_JE_SR_JA_ME_patologias()).isEmpty();
        JE_JE_SR_JA_ME_PlanService.JE_JE_SR_JA_ME_Totales t = plan.JE_JE_SR_JA_ME_totales(paciente);
        assertThat(t.realizado()).isEqualByComparingTo("90");
        assertThat(t.pendiente()).isEqualByComparingTo("0");
        assertThat(t.JE_JE_SR_JA_ME_avance()).isEqualTo(1.0);
    }

    @Test
    void JE_JE_SR_JA_ME_realizarUnaExtraccionDejaLaPiezaAusente() {
        clinica.JE_JE_SR_JA_ME_registrar(paciente, 38, PIEZA, EXTRACCION_INDICADA, null);
        plan.JE_JE_SR_JA_ME_generarDesdeOdontograma(paciente);
        plan.JE_JE_SR_JA_ME_realizar(plan.JE_JE_SR_JA_ME_items(paciente).get(0));
        assertThat(clinica.JE_JE_SR_JA_ME_odontograma(paciente).JE_JE_SR_JA_ME_pieza(38).JE_JE_SR_JA_ME_ausente()).isTrue();
    }

    @Test
    void JE_JE_SR_JA_ME_elPrecioQuedaCongeladoAunqueCambieElCatalogo() {
        clinica.JE_JE_SR_JA_ME_registrar(paciente, 16, OCLUSAL, CARIES, null);
        plan.JE_JE_SR_JA_ME_generarDesdeOdontograma(paciente);
        var obt = tratamientos.findByCodigo("OBT").orElseThrow();
        obt.setPrecio(new BigDecimal("200"));
        tratamientos.save(obt);
        assertThat(plan.JE_JE_SR_JA_ME_items(paciente).get(0).getPrecio()).isEqualByComparingTo("90");
    }

    @Test
    void JE_JE_SR_JA_ME_unaObturacionManualExigeElegirLaCara() {
        var obt = tratamientos.findByCodigo("OBT").orElseThrow();
        assertThatThrownBy(() -> plan.JE_JE_SR_JA_ME_agregar(paciente, obt, 16, null))
                .isInstanceOf(JE_JE_SR_JA_ME_NegocioException.class)
                .hasMessageContaining("cara");
        assertThat(plan.JE_JE_SR_JA_ME_agregar(paciente, obt, 16, DISTAL).getSuperficie()).isEqualTo(DISTAL);
    }

    @Test
    void JE_JE_SR_JA_ME_cancelarYReabrir() {
        var end = tratamientos.findByCodigo("END").orElseThrow();
        JE_JE_SR_JA_ME_ItemPlan i = plan.JE_JE_SR_JA_ME_agregar(paciente, end, 46, null);
        assertThat(plan.JE_JE_SR_JA_ME_cancelar(i).getEstado()).isEqualTo(JE_JE_SR_JA_ME_EstadoItem.CANCELADO);
        assertThatThrownBy(() -> plan.JE_JE_SR_JA_ME_realizar(i)).isInstanceOf(JE_JE_SR_JA_ME_NegocioException.class);
        assertThat(plan.JE_JE_SR_JA_ME_reabrir(i).getEstado()).isEqualTo(JE_JE_SR_JA_ME_EstadoItem.PENDIENTE);
    }
}
