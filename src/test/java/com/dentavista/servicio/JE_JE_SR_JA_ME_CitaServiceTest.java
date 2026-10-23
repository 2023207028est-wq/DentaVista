package com.dentavista.servicio;

import com.dentavista.JE_JE_SR_JA_ME_IntegracionBase;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Cita;
import com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Rol;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Agenda: horario, cruces de horario y permisos de pacientes con acceso propio. */
class JE_JE_SR_JA_ME_CitaServiceTest extends JE_JE_SR_JA_ME_IntegracionBase {

    @Autowired private JE_JE_SR_JA_ME_CitaService citas;
    @Autowired private JE_JE_SR_JA_ME_UsuarioService usuariosService;
    @Autowired private JE_JE_SR_JA_ME_ClinicaService clinica;

    private JE_JE_SR_JA_ME_Usuario doctor;
    private JE_JE_SR_JA_ME_Paciente paciente;
    /** Un martes futuro: siempre hay atención mañana y tarde. */
    private final LocalDate martes = LocalDate.now().plusWeeks(1).with(DayOfWeek.TUESDAY);

    @BeforeEach
    void JE_JE_SR_JA_ME_preparar() {
        doctor = JE_JE_SR_JA_ME_iniciarSesion(JE_JE_SR_JA_ME_Rol.ODONTOLOGO);
        JE_JE_SR_JA_ME_iniciarSesion(JE_JE_SR_JA_ME_Rol.ASISTENTE);
        paciente = JE_JE_SR_JA_ME_pacienteNuevo();
    }

    private LocalDateTime JE_JE_SR_JA_ME_a(int h, int m) {
        return martes.atTime(h, m);
    }

    @Test
    void JE_JE_SR_JA_ME_laRecepcionAgendaCitasConfirmadas() {
        JE_JE_SR_JA_ME_Cita c = citas.JE_JE_SR_JA_ME_agendar(paciente, doctor, JE_JE_SR_JA_ME_a(9, 0), 30, "Control", null);
        assertThat(c.getEstado()).isEqualTo(JE_JE_SR_JA_ME_EstadoCita.CONFIRMADA);
        assertThat(c.JE_JE_SR_JA_ME_fin()).isEqualTo(JE_JE_SR_JA_ME_a(9, 30));
    }

    @Test
    void JE_JE_SR_JA_ME_reprogramarMueveLaCitaYRespetaLosCruces() {
        JE_JE_SR_JA_ME_Cita c = citas.JE_JE_SR_JA_ME_agendar(paciente, doctor, JE_JE_SR_JA_ME_a(9, 0), 60, "Control", null);
        // Puede moverse dentro de su propio horario (no choca consigo misma).
        JE_JE_SR_JA_ME_Cita movida = citas.JE_JE_SR_JA_ME_reprogramar(c, doctor, JE_JE_SR_JA_ME_a(9, 30), 60);
        assertThat(movida.getInicio()).isEqualTo(JE_JE_SR_JA_ME_a(9, 30));
        assertThat(citas.JE_JE_SR_JA_ME_horariosLibres(doctor, martes, 60)).doesNotContain(LocalTime.of(9, 30));

        JE_JE_SR_JA_ME_Cita otra = citas.JE_JE_SR_JA_ME_agendar(JE_JE_SR_JA_ME_pacienteNuevo(), doctor, JE_JE_SR_JA_ME_a(11, 0), 30, null, null);
        assertThatThrownBy(() -> citas.JE_JE_SR_JA_ME_reprogramar(otra, doctor, JE_JE_SR_JA_ME_a(10, 0), 30))
                .isInstanceOf(JE_JE_SR_JA_ME_NegocioException.class).hasMessageContaining("ya tiene una cita");

        citas.JE_JE_SR_JA_ME_cancelar(otra);
        assertThatThrownBy(() -> citas.JE_JE_SR_JA_ME_reprogramar(otra, doctor, JE_JE_SR_JA_ME_a(12, 0), 30)).hasMessageContaining("cancelada");
    }

    @Test
    void JE_JE_SR_JA_ME_unOdontologoNoPuedeTenerDosCitasCruzadas() {
        citas.JE_JE_SR_JA_ME_agendar(paciente, doctor, JE_JE_SR_JA_ME_a(9, 0), 60, null, null);
        JE_JE_SR_JA_ME_Paciente otro = JE_JE_SR_JA_ME_pacienteNuevo();
        assertThatThrownBy(() -> citas.JE_JE_SR_JA_ME_agendar(otro, doctor, JE_JE_SR_JA_ME_a(9, 30), 30, null, null))
                .isInstanceOf(JE_JE_SR_JA_ME_NegocioException.class).hasMessageContaining("ya tiene una cita");
        assertThat(citas.JE_JE_SR_JA_ME_agendar(otro, doctor, JE_JE_SR_JA_ME_a(10, 0), 30, null, null)).isNotNull();
    }

    @Test
    void JE_JE_SR_JA_ME_respetaElHorarioDeAtencion() {
        assertThatThrownBy(() -> citas.JE_JE_SR_JA_ME_agendar(paciente, doctor, JE_JE_SR_JA_ME_a(13, 30), 30, null, null))
                .hasMessageContaining("Fuera del horario");
        assertThatThrownBy(() -> citas.JE_JE_SR_JA_ME_agendar(paciente, doctor, JE_JE_SR_JA_ME_a(12, 30), 60, null, null))
                .hasMessageContaining("Fuera del horario");
        LocalDateTime sabadoTarde = martes.with(DayOfWeek.SATURDAY).atTime(16, 0);
        assertThatThrownBy(() -> citas.JE_JE_SR_JA_ME_agendar(paciente, doctor, sabadoTarde, 30, null, null))
                .hasMessageContaining("Fuera del horario");
        assertThatThrownBy(() -> citas.JE_JE_SR_JA_ME_agendar(paciente, doctor, LocalDateTime.now().minusDays(1), 30, null, null))
                .hasMessageContaining("pasado");
    }

    @Test
    void JE_JE_SR_JA_ME_losHorariosLibresExcluyenLasCitasYLaPausa() {
        citas.JE_JE_SR_JA_ME_agendar(paciente, doctor, JE_JE_SR_JA_ME_a(9, 0), 60, null, null);
        var libres = citas.JE_JE_SR_JA_ME_horariosLibres(doctor, martes, 30);
        assertThat(libres).doesNotContain(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(13, 0), LocalTime.of(14, 0))
                .contains(LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(15, 0), LocalTime.of(18, 30));
    }

    @Test
    void JE_JE_SR_JA_ME_unaCitaCanceladaLiberaElHorario() {
        JE_JE_SR_JA_ME_Cita c = citas.JE_JE_SR_JA_ME_agendar(paciente, doctor, JE_JE_SR_JA_ME_a(11, 0), 30, null, null);
        citas.JE_JE_SR_JA_ME_cancelar(c);
        assertThat(citas.JE_JE_SR_JA_ME_horariosLibres(doctor, martes, 30)).contains(LocalTime.of(11, 0));
        assertThatThrownBy(() -> citas.JE_JE_SR_JA_ME_marcarAtendida(c)).hasMessageContaining("cancelada");
    }

    @Test
    void JE_JE_SR_JA_ME_elPacienteSolicitaSusCitasYSoloVeSuFicha() {
        JE_JE_SR_JA_ME_Usuario acceso = usuariosService.JE_JE_SR_JA_ME_crearAccesoPaciente(paciente, null, "clave123");
        assertThat(acceso.getUsuario()).isEqualTo(paciente.getDni());
        sesion.JE_JE_SR_JA_ME_cerrar();
        usuariosService.JE_JE_SR_JA_ME_autenticar(paciente.getDni(), "clave123");

        JE_JE_SR_JA_ME_Cita solicitada = citas.JE_JE_SR_JA_ME_agendar(paciente, doctor, JE_JE_SR_JA_ME_a(16, 0), 30, "Me duele una muela", null);
        assertThat(solicitada.getEstado()).isEqualTo(JE_JE_SR_JA_ME_EstadoCita.SOLICITADA);
        assertThat(clinica.JE_JE_SR_JA_ME_odontograma(paciente)).isNotNull();

        JE_JE_SR_JA_ME_Paciente ajeno = pacientes.save(JE_JE_SR_JA_ME_Paciente.builder().dni("87654321").nombres("Otro").apellidos("Paciente").build());
        assertThatThrownBy(() -> clinica.JE_JE_SR_JA_ME_odontograma(ajeno)).hasMessageContaining("propia ficha");
        assertThatThrownBy(() -> citas.JE_JE_SR_JA_ME_agendar(ajeno, doctor, JE_JE_SR_JA_ME_a(17, 0), 30, null, null)).hasMessageContaining("propia ficha");
        assertThatThrownBy(() -> citas.JE_JE_SR_JA_ME_confirmar(solicitada)).hasMessageContaining("personal");

        JE_JE_SR_JA_ME_iniciarSesion(JE_JE_SR_JA_ME_Rol.ASISTENTE);
        assertThat(citas.JE_JE_SR_JA_ME_confirmar(solicitada).getEstado()).isEqualTo(JE_JE_SR_JA_ME_EstadoCita.CONFIRMADA);
        assertThat(citas.JE_JE_SR_JA_ME_porConfirmar()).isZero();
    }
}
