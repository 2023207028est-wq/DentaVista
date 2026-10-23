package com.dentavista.servicio;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Rol;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Usuario;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class JE_JE_SR_JA_ME_UsuarioService {

    private final JE_JE_SR_JA_ME_UsuarioRepository usuarios;
    private final JE_JE_SR_JA_ME_SesionService sesion;
    private final JE_JE_SR_JA_ME_Validacion validacion;

    /** Verifica credenciales; el mismo mensaje para usuario o contraseña incorrectos. */
    public JE_JE_SR_JA_ME_Usuario JE_JE_SR_JA_ME_autenticar(String usuario, String contrasena) {
        if (usuario == null || usuario.isBlank() || contrasena == null || contrasena.isEmpty()) {
            throw new JE_JE_SR_JA_ME_NegocioException("Escribe tu usuario y contraseña.");
        }
        JE_JE_SR_JA_ME_Usuario u = usuarios.findByUsuarioIgnoreCase(usuario.trim())
                .filter(x -> JE_JE_SR_JA_ME_Seguridad.JE_JE_SR_JA_ME_coincide(contrasena, x.getContrasena()))
                .orElseThrow(() -> new JE_JE_SR_JA_ME_NegocioException("Usuario o contraseña incorrectos."));
        if (u.getRol() == JE_JE_SR_JA_ME_Rol.PACIENTE && u.getPaciente() == null) {
            throw new JE_JE_SR_JA_ME_NegocioException("Este acceso de paciente no está vinculado a ninguna ficha.");
        }
        if (!u.isActivo()) {
            throw new JE_JE_SR_JA_ME_NegocioException("Tu cuenta está desactivada. Habla con el administrador del consultorio.");
        }
        u.setUltimoAcceso(LocalDateTime.now());
        JE_JE_SR_JA_ME_Usuario guardado = usuarios.save(u);
        sesion.JE_JE_SR_JA_ME_iniciar(guardado);
        return guardado;
    }

    /** Personal del consultorio (los accesos de pacientes se gestionan desde su ficha). */
    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_Usuario> JE_JE_SR_JA_ME_listar() {
        return usuarios.findByRolNotOrderByNombreAsc(JE_JE_SR_JA_ME_Rol.PACIENTE);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<JE_JE_SR_JA_ME_Usuario> JE_JE_SR_JA_ME_accesoDe(com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente p) {
        return usuarios.findByPaciente(p);
    }

    /**
     * Crea (o renueva la contraseña de) el acceso de un paciente. Lo hace el personal del
     * consultorio, por ejemplo en recepción; el usuario por defecto es el DNI.
     */
    public JE_JE_SR_JA_ME_Usuario JE_JE_SR_JA_ME_crearAccesoPaciente(com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente p, String login, String contrasena) {
        sesion.JE_JE_SR_JA_ME_exigirPersonal();
        if (contrasena == null || contrasena.length() < 6) {
            throw new JE_JE_SR_JA_ME_NegocioException("La contraseña debe tener al menos 6 caracteres.");
        }
        String l = login == null || login.isBlank() ? p.getDni() : login.trim();
        JE_JE_SR_JA_ME_Usuario u = usuarios.findByPaciente(p).orElseGet(() -> JE_JE_SR_JA_ME_Usuario.builder()
                .rol(JE_JE_SR_JA_ME_Rol.PACIENTE).paciente(p).activo(true).build());
        usuarios.findByUsuarioIgnoreCase(l)
                .filter(x -> !x.getId().equals(u.getId()))
                .ifPresent(x -> {
                    throw new JE_JE_SR_JA_ME_NegocioException("El usuario \"" + l + "\" ya existe.");
                });
        u.setUsuario(l);
        u.setNombre(p.JE_JE_SR_JA_ME_nombreCompleto());
        u.setEmail(p.getEmail());
        u.setContrasena(JE_JE_SR_JA_ME_Seguridad.JE_JE_SR_JA_ME_hashear(contrasena));
        u.setActivo(true);
        validacion.JE_JE_SR_JA_ME_validar(u);
        return usuarios.save(u);
    }

    /**
     * Crea o actualiza un usuario. {@code contrasena} vacía al editar = se conserva la actual.
     */
    public JE_JE_SR_JA_ME_Usuario JE_JE_SR_JA_ME_guardar(JE_JE_SR_JA_ME_Usuario u, String contrasena) {
        sesion.JE_JE_SR_JA_ME_exigirAdministrador();
        boolean nuevo = u.getId() == null;
        if (nuevo && (contrasena == null || contrasena.length() < 6)) {
            throw new JE_JE_SR_JA_ME_NegocioException("La contraseña debe tener al menos 6 caracteres.");
        }
        if (!nuevo && contrasena != null && !contrasena.isEmpty() && contrasena.length() < 6) {
            throw new JE_JE_SR_JA_ME_NegocioException("La nueva contraseña debe tener al menos 6 caracteres.");
        }
        usuarios.findByUsuarioIgnoreCase(u.getUsuario() == null ? "" : u.getUsuario().trim())
                .filter(x -> !x.getId().equals(u.getId()))
                .ifPresent(x -> {
                    throw new JE_JE_SR_JA_ME_NegocioException("El usuario \"" + x.getUsuario() + "\" ya existe.");
                });
        if (!nuevo) {
            JE_JE_SR_JA_ME_Usuario actual = usuarios.findById(u.getId()).orElseThrow();
            boolean dejaSinAdmin = actual.getRol() == JE_JE_SR_JA_ME_Rol.ADMIN && actual.isActivo()
                    && (u.getRol() != JE_JE_SR_JA_ME_Rol.ADMIN || !u.isActivo())
                    && usuarios.countByRolAndActivoTrue(JE_JE_SR_JA_ME_Rol.ADMIN) <= 1;
            if (dejaSinAdmin) {
                throw new JE_JE_SR_JA_ME_NegocioException("Debe quedar al menos un administrador activo.");
            }
            u.setContrasena(actual.getContrasena());
        }
        if (contrasena != null && !contrasena.isEmpty()) {
            u.setContrasena(JE_JE_SR_JA_ME_Seguridad.JE_JE_SR_JA_ME_hashear(contrasena));
        }
        u.setUsuario(u.getUsuario().trim());
        validacion.JE_JE_SR_JA_ME_validar(u);
        return usuarios.save(u);
    }
}
