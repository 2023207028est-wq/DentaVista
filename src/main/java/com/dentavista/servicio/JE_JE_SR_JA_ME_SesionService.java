package com.dentavista.servicio;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Rol;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Usuario;
import org.springframework.stereotype.Component;

/** Usuario autenticado durante la vida de la aplicación. */
@Component
public class JE_JE_SR_JA_ME_SesionService {

    private JE_JE_SR_JA_ME_Usuario usuario;

    public void JE_JE_SR_JA_ME_iniciar(JE_JE_SR_JA_ME_Usuario usuario) {
        this.usuario = usuario;
    }

    public void JE_JE_SR_JA_ME_cerrar() {
        this.usuario = null;
    }

    public JE_JE_SR_JA_ME_Usuario JE_JE_SR_JA_ME_getUsuario() {
        return usuario;
    }

    public boolean JE_JE_SR_JA_ME_haySesion() {
        return usuario != null;
    }

    public String JE_JE_SR_JA_ME_nombre() {
        return usuario == null ? "Sistema" : usuario.getNombre();
    }

    public boolean JE_JE_SR_JA_ME_puedeRegistrarClinica() {
        return usuario != null && usuario.getRol().JE_JE_SR_JA_ME_puedeRegistrarClinica();
    }

    public boolean JE_JE_SR_JA_ME_puedeAdministrar() {
        return usuario != null && usuario.getRol().JE_JE_SR_JA_ME_puedeAdministrar();
    }

    public void JE_JE_SR_JA_ME_exigirClinica() {
        if (!JE_JE_SR_JA_ME_puedeRegistrarClinica()) {
            throw new JE_JE_SR_JA_ME_NegocioException("Tu rol (" + (usuario == null ? "sin sesión" : usuario.getRol().JE_JE_SR_JA_ME_getEtiqueta())
                    + ") no permite modificar la historia clínica. Pide a un odontólogo que lo registre.");
        }
    }

    public boolean JE_JE_SR_JA_ME_esPaciente() {
        return usuario != null && usuario.getRol() == JE_JE_SR_JA_ME_Rol.PACIENTE;
    }

    public boolean JE_JE_SR_JA_ME_esPersonal() {
        return usuario != null && usuario.getRol().JE_JE_SR_JA_ME_esPersonal();
    }

    /** Ficha del paciente que inició sesión (null si es personal del consultorio). */
    public JE_JE_SR_JA_ME_Paciente JE_JE_SR_JA_ME_pacientePropio() {
        return JE_JE_SR_JA_ME_esPaciente() ? usuario.getPaciente() : null;
    }

    /**
     * Un paciente solo puede ver su propia ficha. Sin sesión (procesos internos como
     * los datos de demostración) o con personal del consultorio no hay restricción.
     */
    public void JE_JE_SR_JA_ME_exigirAccesoA(JE_JE_SR_JA_ME_Paciente p) {
        if (JE_JE_SR_JA_ME_esPaciente() && (usuario.getPaciente() == null || p == null
                || !usuario.getPaciente().getId().equals(p.getId()))) {
            throw new JE_JE_SR_JA_ME_NegocioException("Solo puedes ver tu propia ficha.");
        }
    }

    public void JE_JE_SR_JA_ME_exigirPersonal() {
        if (!JE_JE_SR_JA_ME_esPersonal()) {
            throw new JE_JE_SR_JA_ME_NegocioException("Esta sección es solo para el personal del consultorio.");
        }
    }

    public void JE_JE_SR_JA_ME_exigirAdministrador() {
        if (!JE_JE_SR_JA_ME_puedeAdministrar()) {
            throw new JE_JE_SR_JA_ME_NegocioException("Solo un administrador puede realizar esta acción.");
        }
    }
}
