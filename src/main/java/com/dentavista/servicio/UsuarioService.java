package com.dentavista.servicio;

import com.dentavista.modelo.Rol;
import com.dentavista.modelo.Usuario;
import com.dentavista.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final SesionService sesion;
    private final Validacion validacion;

    /** Verifica credenciales; el mismo mensaje para usuario o contraseña incorrectos. */
    public Usuario autenticar(String usuario, String contrasena) {
        if (usuario == null || usuario.isBlank() || contrasena == null || contrasena.isEmpty()) {
            throw new NegocioException("Escribe tu usuario y contraseña.");
        }
        Usuario u = usuarios.findByUsuarioIgnoreCase(usuario.trim())
                .filter(x -> Seguridad.coincide(contrasena, x.getContrasena()))
                .orElseThrow(() -> new NegocioException("Usuario o contraseña incorrectos."));
        if (u.getRol() == Rol.PACIENTE && u.getPaciente() == null) {
            throw new NegocioException("Este acceso de paciente no está vinculado a ninguna ficha.");
        }
        if (!u.isActivo()) {
            throw new NegocioException("Tu cuenta está desactivada. Habla con el administrador del consultorio.");
        }
        u.setUltimoAcceso(LocalDateTime.now());
        Usuario guardado = usuarios.save(u);
        sesion.iniciar(guardado);
        return guardado;
    }

    /** Personal del consultorio (los accesos de pacientes se gestionan desde su ficha). */
    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.findByRolNotOrderByNombreAsc(Rol.PACIENTE);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Usuario> accesoDe(com.dentavista.modelo.Paciente p) {
        return usuarios.findByPaciente(p);
    }

    /**
     * Crea (o renueva la contraseña de) el acceso de un paciente. Lo hace el personal del
     * consultorio, por ejemplo en recepción; el usuario por defecto es el DNI.
     */
    public Usuario crearAccesoPaciente(com.dentavista.modelo.Paciente p, String login, String contrasena) {
        sesion.exigirPersonal();
        if (contrasena == null || contrasena.length() < 6) {
            throw new NegocioException("La contraseña debe tener al menos 6 caracteres.");
        }
        String l = login == null || login.isBlank() ? p.getDni() : login.trim();
        Usuario u = usuarios.findByPaciente(p).orElseGet(() -> Usuario.builder()
                .rol(Rol.PACIENTE).paciente(p).activo(true).build());
        usuarios.findByUsuarioIgnoreCase(l)
                .filter(x -> !x.getId().equals(u.getId()))
                .ifPresent(x -> {
                    throw new NegocioException("El usuario \"" + l + "\" ya existe.");
                });
        u.setUsuario(l);
        u.setNombre(p.nombreCompleto());
        u.setEmail(p.getEmail());
        u.setContrasena(Seguridad.hashear(contrasena));
        u.setActivo(true);
        validacion.validar(u);
        return usuarios.save(u);
    }

    /**
     * Crea o actualiza un usuario. {@code contrasena} vacía al editar = se conserva la actual.
     */
    public Usuario guardar(Usuario u, String contrasena) {
        sesion.exigirAdministrador();
        boolean nuevo = u.getId() == null;
        if (nuevo && (contrasena == null || contrasena.length() < 6)) {
            throw new NegocioException("La contraseña debe tener al menos 6 caracteres.");
        }
        if (!nuevo && contrasena != null && !contrasena.isEmpty() && contrasena.length() < 6) {
            throw new NegocioException("La nueva contraseña debe tener al menos 6 caracteres.");
        }
        usuarios.findByUsuarioIgnoreCase(u.getUsuario() == null ? "" : u.getUsuario().trim())
                .filter(x -> !x.getId().equals(u.getId()))
                .ifPresent(x -> {
                    throw new NegocioException("El usuario \"" + x.getUsuario() + "\" ya existe.");
                });
        if (!nuevo) {
            Usuario actual = usuarios.findById(u.getId()).orElseThrow();
            boolean dejaSinAdmin = actual.getRol() == Rol.ADMIN && actual.isActivo()
                    && (u.getRol() != Rol.ADMIN || !u.isActivo())
                    && usuarios.countByRolAndActivoTrue(Rol.ADMIN) <= 1;
            if (dejaSinAdmin) {
                throw new NegocioException("Debe quedar al menos un administrador activo.");
            }
            u.setContrasena(actual.getContrasena());
        }
        if (contrasena != null && !contrasena.isEmpty()) {
            u.setContrasena(Seguridad.hashear(contrasena));
        }
        u.setUsuario(u.getUsuario().trim());
        validacion.validar(u);
        return usuarios.save(u);
    }
}
