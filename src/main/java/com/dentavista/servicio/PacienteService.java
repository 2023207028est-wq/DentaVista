package com.dentavista.servicio;

import com.dentavista.modelo.Consulta;
import com.dentavista.modelo.Paciente;
import com.dentavista.repositorio.ConsultaRepository;
import com.dentavista.repositorio.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PacienteService {

    private final PacienteRepository pacientes;
    private final ConsultaRepository consultas;
    private final Validacion validacion;
    private final SesionService sesion;

    @Transactional(readOnly = true)
    public List<Paciente> buscar(String texto) {
        sesion.exigirPersonal();
        return pacientes.buscar(texto == null ? "" : texto.trim());
    }

    @Transactional(readOnly = true)
    public Paciente obtener(Long id) {
        Paciente p = pacientes.findById(id)
                .orElseThrow(() -> new NegocioException("El paciente ya no existe."));
        sesion.exigirAccesoA(p);
        return p;
    }

    public Paciente guardar(Paciente p) {
        if (sesion.haySesion()) {
            sesion.exigirPersonal();
        }
        p.setDni(p.getDni() == null ? null : p.getDni().trim());
        p.setNombres(limpiar(p.getNombres()));
        p.setApellidos(limpiar(p.getApellidos()));
        validacion.validar(p);
        pacientes.findByDni(p.getDni())
                .filter(otro -> !otro.getId().equals(p.getId()))
                .ifPresent(otro -> {
                    throw new NegocioException("Ya existe un paciente con DNI " + p.getDni()
                            + ": " + otro.nombreCompleto() + ".");
                });
        return pacientes.save(p);
    }

    /** Baja lógica: la historia clínica se conserva por obligación legal. */
    public void archivar(Paciente p) {
        Paciente actual = obtener(p.getId());
        actual.setActivo(false);
        pacientes.save(actual);
    }

    @Transactional(readOnly = true)
    public Optional<LocalDateTime> ultimaVisita(Paciente p) {
        return consultas.findFirstByPacienteOrderByFechaDesc(p).map(Consulta::getFecha);
    }

    private static String limpiar(String s) {
        return s == null ? null : s.trim().replaceAll("\\s+", " ");
    }
}
