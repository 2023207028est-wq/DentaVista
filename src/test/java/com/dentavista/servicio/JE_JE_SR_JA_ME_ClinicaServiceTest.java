package com.dentavista.servicio;

import com.dentavista.JE_JE_SR_JA_ME_IntegracionBase;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Hallazgo;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental.*;
import static com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Registro de hallazgos: validaciones clínicas, consulta del día y deshacer. */
class JE_JE_SR_JA_ME_ClinicaServiceTest extends JE_JE_SR_JA_ME_IntegracionBase {

    @Autowired
    private JE_JE_SR_JA_ME_ClinicaService clinica;

    private JE_JE_SR_JA_ME_Paciente paciente;

    @BeforeEach
    void JE_JE_SR_JA_ME_preparar() {
        JE_JE_SR_JA_ME_iniciarSesion(JE_JE_SR_JA_ME_Rol.ODONTOLOGO);
        paciente = JE_JE_SR_JA_ME_pacienteNuevo();
    }

    @Test
    void JE_JE_SR_JA_ME_registraUnaCariesYAbreLaConsultaDelDia() {
        JE_JE_SR_JA_ME_Hallazgo h = clinica.JE_JE_SR_JA_ME_registrar(paciente, 16, OCLUSAL, CARIES, "Lesión profunda");
        assertThat(h.getConsulta()).isNotNull();
        assertThat(h.getNota()).isEqualTo("Lesión profunda");
        assertThat(clinica.JE_JE_SR_JA_ME_odontograma(paciente).JE_JE_SR_JA_ME_pieza(16).JE_JE_SR_JA_ME_cara(OCLUSAL)).isEqualTo(CARIES);

        clinica.JE_JE_SR_JA_ME_registrar(paciente, 26, MESIAL, CARIES, null);
        assertThat(clinica.JE_JE_SR_JA_ME_consultas(paciente)).hasSize(1);
    }

    @Test
    void JE_JE_SR_JA_ME_repetirElMismoEstadoNoCreaRegistros() {
        clinica.JE_JE_SR_JA_ME_registrar(paciente, 16, OCLUSAL, CARIES, null);
        assertThat(clinica.JE_JE_SR_JA_ME_registrar(paciente, 16, OCLUSAL, CARIES, null)).isNull();
        assertThat(clinica.JE_JE_SR_JA_ME_hallazgos(paciente)).hasSize(1);
    }

    @Test
    void JE_JE_SR_JA_ME_unaCariesNoSePuedeRegistrarEnLaPiezaCompleta() {
        assertThatThrownBy(() -> clinica.JE_JE_SR_JA_ME_registrar(paciente, 16, PIEZA, CARIES, null))
                .isInstanceOf(JE_JE_SR_JA_ME_NegocioException.class)
                .hasMessageContaining("cara");
    }

    @Test
    void JE_JE_SR_JA_ME_unaCoronaNoSeRegistraEnUnaSolaCara() {
        assertThatThrownBy(() -> clinica.JE_JE_SR_JA_ME_registrar(paciente, 11, VESTIBULAR, CORONA, null))
                .isInstanceOf(JE_JE_SR_JA_ME_NegocioException.class)
                .hasMessageContaining("pieza completa");
    }

    @Test
    void JE_JE_SR_JA_ME_unaPiezaAusenteNoTieneCaras() {
        clinica.JE_JE_SR_JA_ME_registrar(paciente, 36, PIEZA, AUSENTE, null);
        assertThatThrownBy(() -> clinica.JE_JE_SR_JA_ME_registrar(paciente, 36, OCLUSAL, CARIES, null))
                .isInstanceOf(JE_JE_SR_JA_ME_NegocioException.class)
                .hasMessageContaining("36");
    }

    @Test
    void JE_JE_SR_JA_ME_deshacerEliminaElUltimoRegistroDeHoy() {
        clinica.JE_JE_SR_JA_ME_registrar(paciente, 16, OCLUSAL, CARIES, null);
        clinica.JE_JE_SR_JA_ME_registrar(paciente, 17, OCLUSAL, CARIES, null);
        JE_JE_SR_JA_ME_Hallazgo deshecho = clinica.JE_JE_SR_JA_ME_deshacerUltimo(paciente);
        assertThat(deshecho.getPieza()).isEqualTo(17);
        assertThat(clinica.JE_JE_SR_JA_ME_odontograma(paciente).JE_JE_SR_JA_ME_pieza(17).JE_JE_SR_JA_ME_sana()).isTrue();
        assertThat(clinica.JE_JE_SR_JA_ME_odontograma(paciente).JE_JE_SR_JA_ME_pieza(16).JE_JE_SR_JA_ME_cara(OCLUSAL)).isEqualTo(CARIES);
    }

    @Test
    void JE_JE_SR_JA_ME_deshacerElUnicoRegistroEliminaLaConsultaVacia() {
        clinica.JE_JE_SR_JA_ME_registrar(paciente, 16, OCLUSAL, CARIES, null);
        clinica.JE_JE_SR_JA_ME_deshacerUltimo(paciente);
        assertThat(clinica.JE_JE_SR_JA_ME_consultas(paciente)).isEmpty();
        assertThat(clinica.JE_JE_SR_JA_ME_deshacerUltimo(paciente)).isNull();
    }

    @Test
    void JE_JE_SR_JA_ME_laRecepcionNoPuedeModificarLaHistoriaClinica() {
        JE_JE_SR_JA_ME_iniciarSesion(JE_JE_SR_JA_ME_Rol.ASISTENTE);
        assertThatThrownBy(() -> clinica.JE_JE_SR_JA_ME_registrar(paciente, 16, OCLUSAL, CARIES, null))
                .isInstanceOf(JE_JE_SR_JA_ME_NegocioException.class)
                .hasMessageContaining("no permite");
    }
}
