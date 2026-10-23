package com.dentavista.servicio;

import com.dentavista.JE_JE_SR_JA_ME_IntegracionBase;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Cita;
import com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita;
import com.dentavista.modelo.JE_JE_SR_JA_ME_MedioPago;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Rol;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

import static com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental.CARIES;
import static com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental.OBTURADO;
import static com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie.OCLUSAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Dar de baja (archivar) a pacientes que ya no necesitan atención. */
class JE_JE_SR_JA_ME_ArchivoPacienteTest extends JE_JE_SR_JA_ME_IntegracionBase {

    @Autowired private JE_JE_SR_JA_ME_PacienteService servicio;
    @Autowired private JE_JE_SR_JA_ME_PagoService pagos;
    @Autowired private JE_JE_SR_JA_ME_ClinicaService clinica;
    @Autowired private JE_JE_SR_JA_ME_PlanService planes;
    @Autowired private JE_JE_SR_JA_ME_CitaService citas;

    private JE_JE_SR_JA_ME_Usuario doctor;
    private JE_JE_SR_JA_ME_Paciente paciente;

    @BeforeEach
    void JE_JE_SR_JA_ME_preparar() {
        doctor = JE_JE_SR_JA_ME_iniciarSesion(JE_JE_SR_JA_ME_Rol.ODONTOLOGO);
        JE_JE_SR_JA_ME_tratamiento("OBT", "90", OBTURADO, true);
        paciente = JE_JE_SR_JA_ME_pacienteNuevo();
        // Plan de una obturación, ya realizada: debe S/ 90.
        clinica.JE_JE_SR_JA_ME_registrar(paciente, 16, OCLUSAL, CARIES, null);
        planes.JE_JE_SR_JA_ME_generarDesdeOdontograma(paciente);
        planes.JE_JE_SR_JA_ME_realizar(planes.JE_JE_SR_JA_ME_items(paciente).get(0));
    }

    @Test
    void JE_JE_SR_JA_ME_noSeArchivaAQuienTodaviaDebe() {
        assertThat(servicio.JE_JE_SR_JA_ME_paraDarDeAlta()).doesNotContain(paciente);
        assertThatThrownBy(() -> servicio.JE_JE_SR_JA_ME_archivar(paciente))
                .isInstanceOf(JE_JE_SR_JA_ME_NegocioException.class).hasMessageContaining("debe S/ 90.00");
    }

    @Test
    void JE_JE_SR_JA_ME_quienTerminoYPagoApareceParaDarDeAltaYSePuedeArchivarYReactivar() {
        pagos.JE_JE_SR_JA_ME_registrar(paciente, new BigDecimal("90"), JE_JE_SR_JA_ME_MedioPago.EFECTIVO, null);
        assertThat(servicio.JE_JE_SR_JA_ME_paraDarDeAlta()).contains(paciente);

        servicio.JE_JE_SR_JA_ME_archivar(paciente);
        assertThat(servicio.JE_JE_SR_JA_ME_buscar("")).doesNotContain(paciente);
        assertThat(servicio.JE_JE_SR_JA_ME_archivados("")).contains(paciente);
        assertThat(servicio.JE_JE_SR_JA_ME_paraDarDeAlta()).doesNotContain(paciente);

        servicio.JE_JE_SR_JA_ME_reactivar(paciente);
        assertThat(servicio.JE_JE_SR_JA_ME_buscar("")).contains(paciente);
        assertThat(servicio.JE_JE_SR_JA_ME_archivados("")).doesNotContain(paciente);
    }

    @Test
    void JE_JE_SR_JA_ME_conCitaPorVenirNoEstaParaAltaYAlArchivarseCancela() {
        pagos.JE_JE_SR_JA_ME_registrar(paciente, new BigDecimal("90"), JE_JE_SR_JA_ME_MedioPago.EFECTIVO, null);
        JE_JE_SR_JA_ME_Cita control = citas.JE_JE_SR_JA_ME_agendar(paciente, doctor,
                LocalDate.now().plusWeeks(1).with(DayOfWeek.TUESDAY).atTime(9, 0), 30, "Control", null);
        assertThat(servicio.JE_JE_SR_JA_ME_paraDarDeAlta()).doesNotContain(paciente);

        servicio.JE_JE_SR_JA_ME_archivar(paciente);
        assertThat(citas.JE_JE_SR_JA_ME_dePaciente(paciente)).extracting(JE_JE_SR_JA_ME_Cita::getEstado).containsOnly(JE_JE_SR_JA_ME_EstadoCita.CANCELADA);
        assertThat(control.getId()).isNotNull();
    }
}
