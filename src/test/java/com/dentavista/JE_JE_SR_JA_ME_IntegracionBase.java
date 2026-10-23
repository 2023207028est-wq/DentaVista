package com.dentavista;

import com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Rol;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Tratamiento;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Usuario;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_PacienteRepository;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_TratamientoRepository;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_UsuarioRepository;
import com.dentavista.servicio.JE_JE_SR_JA_ME_Seguridad;
import com.dentavista.servicio.JE_JE_SR_JA_ME_SesionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base de las pruebas de integración: contexto completo de Spring sobre una base
 * SQLite temporal y sin datos de demostración.
 */
@SpringBootTest
public abstract class JE_JE_SR_JA_ME_IntegracionBase {

    private static final AtomicInteger DNI = new AtomicInteger(10000000);

    @Autowired protected JE_JE_SR_JA_ME_PacienteRepository pacientes;
    @Autowired protected JE_JE_SR_JA_ME_UsuarioRepository usuarios;
    @Autowired protected JE_JE_SR_JA_ME_TratamientoRepository tratamientos;
    @Autowired protected JE_JE_SR_JA_ME_SesionService sesion;

    @DynamicPropertySource
    static void JE_JE_SR_JA_ME_propiedades(DynamicPropertyRegistry registro) throws Exception {
        Path carpeta = Files.createTempDirectory("dentavista-test");
        carpeta.toFile().deleteOnExit();
        registro.add("spring.datasource.url", () -> "jdbc:sqlite:" + carpeta.resolve("prueba.db"));
        registro.add("app.demo.enabled", () -> "false");
        registro.add("logging.level.com.dentavista", () -> "WARN");
    }

    protected JE_JE_SR_JA_ME_Usuario JE_JE_SR_JA_ME_iniciarSesion(JE_JE_SR_JA_ME_Rol rol) {
        String login = rol.name().toLowerCase() + DNI.incrementAndGet();
        JE_JE_SR_JA_ME_Usuario u = usuarios.save(JE_JE_SR_JA_ME_Usuario.builder()
                .usuario(login)
                .contrasena(JE_JE_SR_JA_ME_Seguridad.JE_JE_SR_JA_ME_hashear("clave123"))
                .nombre("Usuario " + rol.JE_JE_SR_JA_ME_getEtiqueta())
                .rol(rol)
                .activo(true)
                .build());
        sesion.JE_JE_SR_JA_ME_iniciar(u);
        return u;
    }

    protected JE_JE_SR_JA_ME_Paciente JE_JE_SR_JA_ME_pacienteNuevo() {
        return pacientes.save(JE_JE_SR_JA_ME_Paciente.builder()
                .dni(String.valueOf(DNI.incrementAndGet()))
                .nombres("Paciente")
                .apellidos("De Prueba")
                .fechaNacimiento(LocalDate.of(1990, 5, 20))
                .build());
    }

    protected JE_JE_SR_JA_ME_Tratamiento JE_JE_SR_JA_ME_tratamiento(String codigo, String precio, JE_JE_SR_JA_ME_EstadoDental resultado, boolean porCara) {
        // El contexto (y la base) se comparte entre pruebas: se deja siempre en su estado inicial.
        JE_JE_SR_JA_ME_Tratamiento t = tratamientos.findByCodigo(codigo).orElseGet(JE_JE_SR_JA_ME_Tratamiento::new);
        t.setCodigo(codigo);
        t.setNombre("Tratamiento " + codigo);
        t.setPrecio(new BigDecimal(precio));
        t.setEstadoResultante(resultado);
        t.setPorCara(porCara);
        t.setRequierePieza(true);
        t.setActivo(true);
        return tratamientos.save(t);
    }
}
