package com.dentavista.repositorio;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Tratamiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JE_JE_SR_JA_ME_TratamientoRepository extends JpaRepository<JE_JE_SR_JA_ME_Tratamiento, Long> {

    Optional<JE_JE_SR_JA_ME_Tratamiento> findByCodigo(String codigo);

    List<JE_JE_SR_JA_ME_Tratamiento> findByActivoTrueOrderByNombreAsc();

    List<JE_JE_SR_JA_ME_Tratamiento> findAllByOrderByNombreAsc();
}
