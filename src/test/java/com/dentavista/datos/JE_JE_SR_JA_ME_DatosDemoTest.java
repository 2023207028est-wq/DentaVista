package com.dentavista.datos;

import com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoItem;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_ConsultaRepository;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_PacienteRepository;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_UsuarioRepository;
import com.dentavista.servicio.JE_JE_SR_JA_ME_ClinicaService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_PlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Los datos de demostración se cargan completos y son clínicamente coherentes. */
@SpringBootTest
class JE_JE_SR_JA_ME_DatosDemoTest {

    @Autowired private JE_JE_SR_JA_ME_PacienteRepository pacientes;
    @Autowired private JE_JE_SR_JA_ME_UsuarioRepository usuarios;
    @Autowired private JE_JE_SR_JA_ME_ConsultaRepository consultas;
    @Autowired private JE_JE_SR_JA_ME_ClinicaService clinica;
    @Autowired private JE_JE_SR_JA_ME_PlanService plan;
    @Autowired private JE_JE_SR_JA_ME_DatosDemo demo;

    @DynamicPropertySource
    static void JE_JE_SR_JA_ME_propiedades(DynamicPropertyRegistry registro) throws Exception {
        Path carpeta = Files.createTempDirectory("dentavista-demo");
        carpeta.toFile().deleteOnExit();
        registro.add("spring.datasource.url", () -> "jdbc:sqlite:" + carpeta.resolve("demo.db"));
        registro.add("app.demo.enabled", () -> "true");
    }

    @Test
    void JE_JE_SR_JA_ME_cargaUsuariosPacientesYConsultas() {
        assertThat(usuarios.count()).isEqualTo(5);
        assertThat(pacientes.count()).isEqualTo(12);
        assertThat(consultas.count()).isGreaterThan(25);
    }

    @Test
    void JE_JE_SR_JA_ME_esIdempotente() {
        demo.run();
        assertThat(pacientes.count()).isEqualTo(12);
    }

    @Test
    void JE_JE_SR_JA_ME_cadaPacienteTieneHistoriaYPlanCoherente() {
        for (JE_JE_SR_JA_ME_Paciente p : pacientes.findAll()) {
            assertThat(clinica.JE_JE_SR_JA_ME_consultas(p)).as("consultas de %s", p.JE_JE_SR_JA_ME_nombreCompleto()).isNotEmpty();
            // Toda patología abierta tiene un tratamiento pendiente en el plan.
            long pendientes = plan.JE_JE_SR_JA_ME_items(p).stream().filter(i -> i.getEstado() == JE_JE_SR_JA_ME_EstadoItem.PENDIENTE).count();
            assertThat(pendientes).as("plan de %s", p.JE_JE_SR_JA_ME_nombreCompleto())
                    .isGreaterThanOrEqualTo(clinica.JE_JE_SR_JA_ME_odontograma(p).JE_JE_SR_JA_ME_patologias().size());
        }
    }

    @Test
    void JE_JE_SR_JA_ME_laPacienteDemoMuestraEvolucionEnElTiempo() {
        JE_JE_SR_JA_ME_Paciente valeria = pacientes.findByDni("72845163").orElseThrow();
        var primera = clinica.JE_JE_SR_JA_ME_consultasCronologicas(valeria).get(0);
        var alInicio = clinica.JE_JE_SR_JA_ME_odontograma(valeria, primera.getFecha().plusHours(1));
        var hoy = clinica.JE_JE_SR_JA_ME_odontograma(valeria);
        assertThat(alInicio.JE_JE_SR_JA_ME_pieza(16).JE_JE_SR_JA_ME_cara(com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie.OCLUSAL))
                .isEqualTo(com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental.CARIES);
        assertThat(hoy.JE_JE_SR_JA_ME_pieza(16).JE_JE_SR_JA_ME_cara(com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie.OCLUSAL))
                .isEqualTo(com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental.OBTURADO);
        assertThat(hoy.JE_JE_SR_JA_ME_pieza(46).JE_JE_SR_JA_ME_general()).isEqualTo(com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental.CORONA);
    }
}
