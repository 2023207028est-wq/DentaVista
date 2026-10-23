package com.dentavista.servicio;

import com.dentavista.JE_JE_SR_JA_ME_IntegracionBase;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Medicamento;
import com.dentavista.modelo.JE_JE_SR_JA_ME_MedioPago;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Pago;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Receta;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental.CARIES;
import static com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental.OBTURADO;
import static com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie.OCLUSAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Caja (pagos y saldo) y recetas con alertas de alergia. */
class JE_JE_SR_JA_ME_PagoRecetaTest extends JE_JE_SR_JA_ME_IntegracionBase {

    @Autowired private JE_JE_SR_JA_ME_PagoService pagos;
    @Autowired private JE_JE_SR_JA_ME_RecetaService recetas;
    @Autowired private JE_JE_SR_JA_ME_ClinicaService clinica;
    @Autowired private JE_JE_SR_JA_ME_PlanService planes;

    private JE_JE_SR_JA_ME_Paciente paciente;

    @BeforeEach
    void JE_JE_SR_JA_ME_preparar() {
        JE_JE_SR_JA_ME_iniciarSesion(JE_JE_SR_JA_ME_Rol.ODONTOLOGO);
        JE_JE_SR_JA_ME_tratamiento("OBT", "90", OBTURADO, true);
        paciente = JE_JE_SR_JA_ME_pacienteNuevo();
        // Plan: dos obturaciones (S/ 180); una realizada (S/ 90).
        clinica.JE_JE_SR_JA_ME_registrar(paciente, 16, OCLUSAL, CARIES, null);
        clinica.JE_JE_SR_JA_ME_registrar(paciente, 26, OCLUSAL, CARIES, null);
        planes.JE_JE_SR_JA_ME_generarDesdeOdontograma(paciente);
        planes.JE_JE_SR_JA_ME_realizar(planes.JE_JE_SR_JA_ME_items(paciente).get(0));
    }

    @Test
    void JE_JE_SR_JA_ME_laDeudaEsLoRealizadoMenosLoPagado() {
        JE_JE_SR_JA_ME_PagoService.JE_JE_SR_JA_ME_Cuenta c = pagos.JE_JE_SR_JA_ME_cuenta(paciente);
        assertThat(c.totalPlan()).isEqualByComparingTo("180");
        assertThat(c.JE_JE_SR_JA_ME_deuda()).isEqualByComparingTo("90");

        JE_JE_SR_JA_ME_Pago p = pagos.JE_JE_SR_JA_ME_registrar(paciente, new BigDecimal("50"), JE_JE_SR_JA_ME_MedioPago.YAPE, "Operación 123");
        assertThat(p.getRecibo()).matches("R-\\d{4}-\\d{4}");
        assertThat(pagos.JE_JE_SR_JA_ME_cuenta(paciente).JE_JE_SR_JA_ME_deuda()).isEqualByComparingTo("40");
    }

    @Test
    void JE_JE_SR_JA_ME_pagarElSaldoDejaNadaPorPagar() {
        pagos.JE_JE_SR_JA_ME_registrar(paciente, pagos.JE_JE_SR_JA_ME_cuenta(paciente).JE_JE_SR_JA_ME_porPagarDelPlan(), JE_JE_SR_JA_ME_MedioPago.EFECTIVO, null);
        JE_JE_SR_JA_ME_PagoService.JE_JE_SR_JA_ME_Cuenta c = pagos.JE_JE_SR_JA_ME_cuenta(paciente);
        assertThat(c.JE_JE_SR_JA_ME_porPagarDelPlan()).isEqualByComparingTo("0");
        assertThat(c.JE_JE_SR_JA_ME_deuda()).isEqualByComparingTo("0");
        assertThat(c.pagado()).isEqualByComparingTo("180");
    }

    @Test
    void JE_JE_SR_JA_ME_sePuedePagarPorAdelantadoPeroNoMasQueElPlan() {
        pagos.JE_JE_SR_JA_ME_registrar(paciente, new BigDecimal("150"), JE_JE_SR_JA_ME_MedioPago.EFECTIVO, null);
        JE_JE_SR_JA_ME_PagoService.JE_JE_SR_JA_ME_Cuenta c = pagos.JE_JE_SR_JA_ME_cuenta(paciente);
        assertThat(c.JE_JE_SR_JA_ME_deuda()).isEqualByComparingTo("0");
        assertThat(c.JE_JE_SR_JA_ME_aFavor()).isEqualByComparingTo("60");
        assertThatThrownBy(() -> pagos.JE_JE_SR_JA_ME_registrar(paciente, new BigDecimal("31"), JE_JE_SR_JA_ME_MedioPago.EFECTIVO, null))
                .isInstanceOf(JE_JE_SR_JA_ME_NegocioException.class).hasMessageContaining("Máximo: S/ 30.00");
        assertThatThrownBy(() -> pagos.JE_JE_SR_JA_ME_registrar(paciente, BigDecimal.ZERO, JE_JE_SR_JA_ME_MedioPago.EFECTIVO, null))
                .hasMessageContaining("mayor a cero");
    }

    @Test
    void JE_JE_SR_JA_ME_unReciboAnuladoDejaDeContar() {
        JE_JE_SR_JA_ME_Pago p = pagos.JE_JE_SR_JA_ME_registrar(paciente, new BigDecimal("90"), JE_JE_SR_JA_ME_MedioPago.TARJETA, null);
        pagos.JE_JE_SR_JA_ME_anular(p, "Monto mal digitado");
        assertThat(pagos.JE_JE_SR_JA_ME_cuenta(paciente).JE_JE_SR_JA_ME_deuda()).isEqualByComparingTo("90");
        assertThatThrownBy(() -> pagos.JE_JE_SR_JA_ME_anular(p, "otra vez")).hasMessageContaining("ya está anulado");
    }

    @Test
    void JE_JE_SR_JA_ME_laRecetaAvisaDeLaAlergiaALaPenicilina() {
        paciente.setAlergias("Penicilina");
        pacientes.save(paciente);
        List<JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item> items = List.of(new JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item("Amoxicilina 500 mg", "1 cada 8 horas"));
        assertThatThrownBy(() -> recetas.JE_JE_SR_JA_ME_emitir(paciente, items, null, false))
                .isInstanceOf(JE_JE_SR_JA_ME_NegocioException.class).hasMessageContaining("ALERGIA");
        JE_JE_SR_JA_ME_Receta r = recetas.JE_JE_SR_JA_ME_emitir(paciente, items, null, true);
        assertThat(r.getAlertasAceptadas()).contains("penicilinas");
        assertThat(recetas.JE_JE_SR_JA_ME_emitir(paciente, List.of(new JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item("Clindamicina 300 mg", "")), null, false)
                .getItems().get(0).getIndicacion()).isEqualTo("Según indicación");
    }

    @Test
    void JE_JE_SR_JA_ME_laRecepcionNoEmiteRecetas() {
        JE_JE_SR_JA_ME_iniciarSesion(JE_JE_SR_JA_ME_Rol.ASISTENTE);
        assertThatThrownBy(() -> recetas.JE_JE_SR_JA_ME_emitir(paciente, List.of(new JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item("Paracetamol 500 mg", "x")), null, false))
                .hasMessageContaining("no permite");
    }

    @Test
    void JE_JE_SR_JA_ME_alertasDeContraindicaciones() {
        assertThat(JE_JE_SR_JA_ME_Medicamento.JE_JE_SR_JA_ME_alertas("Ibuprofeno 400 mg", null, "Anticoagulado (warfarina)"))
                .anyMatch(a -> a.contains("sangrado"));
        assertThat(JE_JE_SR_JA_ME_Medicamento.JE_JE_SR_JA_ME_alertas("Naproxeno 550 mg", "Ibuprofeno", null))
                .anyMatch(a -> a.contains("ALERGIA"));
        assertThat(JE_JE_SR_JA_ME_Medicamento.JE_JE_SR_JA_ME_alertas("Ketorolaco 10 mg", null, "Embarazo 2.º trimestre"))
                .anyMatch(a -> a.contains("embarazo"));
        assertThat(JE_JE_SR_JA_ME_Medicamento.JE_JE_SR_JA_ME_alertas("Paracetamol 500 mg", "Penicilina", "Embarazo")).isEmpty();
    }
}
