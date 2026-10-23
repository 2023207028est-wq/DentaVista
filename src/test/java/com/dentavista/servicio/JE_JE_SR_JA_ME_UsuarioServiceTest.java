package com.dentavista.servicio;

import com.dentavista.JE_JE_SR_JA_ME_IntegracionBase;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Rol;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Acceso y gestión de usuarios. */
class JE_JE_SR_JA_ME_UsuarioServiceTest extends JE_JE_SR_JA_ME_IntegracionBase {

    @Autowired
    private JE_JE_SR_JA_ME_UsuarioService servicio;

    @Test
    void JE_JE_SR_JA_ME_laContrasenaSeGuardaConHashYSal() {
        String a = JE_JE_SR_JA_ME_Seguridad.JE_JE_SR_JA_ME_hashear("secreta");
        String b = JE_JE_SR_JA_ME_Seguridad.JE_JE_SR_JA_ME_hashear("secreta");
        assertThat(a).isNotEqualTo(b).doesNotContain("secreta");
        assertThat(JE_JE_SR_JA_ME_Seguridad.JE_JE_SR_JA_ME_coincide("secreta", a)).isTrue();
        assertThat(JE_JE_SR_JA_ME_Seguridad.JE_JE_SR_JA_ME_coincide("otra", a)).isFalse();
    }

    @Test
    void JE_JE_SR_JA_ME_autenticaConCredencialesCorrectas() {
        JE_JE_SR_JA_ME_Usuario u = JE_JE_SR_JA_ME_iniciarSesion(JE_JE_SR_JA_ME_Rol.ODONTOLOGO);
        sesion.JE_JE_SR_JA_ME_cerrar();
        JE_JE_SR_JA_ME_Usuario ok = servicio.JE_JE_SR_JA_ME_autenticar(u.getUsuario(), "clave123");
        assertThat(ok.getUltimoAcceso()).isNotNull();
        assertThat(sesion.JE_JE_SR_JA_ME_getUsuario()).isEqualTo(ok);
    }

    @Test
    void JE_JE_SR_JA_ME_rechazaContrasenaIncorrectaSinRevelarSiElUsuarioExiste() {
        JE_JE_SR_JA_ME_Usuario u = JE_JE_SR_JA_ME_iniciarSesion(JE_JE_SR_JA_ME_Rol.ODONTOLOGO);
        assertThatThrownBy(() -> servicio.JE_JE_SR_JA_ME_autenticar(u.getUsuario(), "mala"))
                .hasMessage("Usuario o contraseña incorrectos.");
        assertThatThrownBy(() -> servicio.JE_JE_SR_JA_ME_autenticar("no.existe", "mala"))
                .hasMessage("Usuario o contraseña incorrectos.");
    }

    @Test
    void JE_JE_SR_JA_ME_soloElAdministradorCreaUsuarios() {
        JE_JE_SR_JA_ME_iniciarSesion(JE_JE_SR_JA_ME_Rol.ODONTOLOGO);
        JE_JE_SR_JA_ME_Usuario nuevo = JE_JE_SR_JA_ME_Usuario.builder().usuario("nuevo.doc").nombre("Nuevo").rol(JE_JE_SR_JA_ME_Rol.ODONTOLOGO).build();
        assertThatThrownBy(() -> servicio.JE_JE_SR_JA_ME_guardar(nuevo, "clave123")).isInstanceOf(JE_JE_SR_JA_ME_NegocioException.class);

        JE_JE_SR_JA_ME_iniciarSesion(JE_JE_SR_JA_ME_Rol.ADMIN);
        assertThat(servicio.JE_JE_SR_JA_ME_guardar(nuevo, "clave123").getId()).isNotNull();
    }
}
