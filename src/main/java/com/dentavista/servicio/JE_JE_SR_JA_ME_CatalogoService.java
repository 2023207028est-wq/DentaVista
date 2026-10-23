package com.dentavista.servicio;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Tratamiento;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_TratamientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Catálogo de tratamientos y precios del consultorio. */
@Service
@RequiredArgsConstructor
@Transactional
public class JE_JE_SR_JA_ME_CatalogoService {

    private final JE_JE_SR_JA_ME_TratamientoRepository tratamientos;
    private final JE_JE_SR_JA_ME_SesionService sesion;
    private final JE_JE_SR_JA_ME_Validacion validacion;

    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_Tratamiento> JE_JE_SR_JA_ME_activos() {
        return tratamientos.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<JE_JE_SR_JA_ME_Tratamiento> JE_JE_SR_JA_ME_todos() {
        return tratamientos.findAllByOrderByNombreAsc();
    }

    public JE_JE_SR_JA_ME_Tratamiento JE_JE_SR_JA_ME_guardar(JE_JE_SR_JA_ME_Tratamiento t) {
        sesion.JE_JE_SR_JA_ME_exigirAdministrador();
        t.setCodigo(t.getCodigo() == null ? null : t.getCodigo().trim().toUpperCase());
        validacion.JE_JE_SR_JA_ME_validar(t);
        tratamientos.findByCodigo(t.getCodigo())
                .filter(o -> !o.getId().equals(t.getId()))
                .ifPresent(o -> {
                    throw new JE_JE_SR_JA_ME_NegocioException("El código " + t.getCodigo() + " ya lo usa \"" + o.getNombre() + "\".");
                });
        return tratamientos.save(t);
    }
}
