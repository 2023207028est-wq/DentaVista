package com.dentavista.repositorio;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JE_JE_SR_JA_ME_PacienteRepository extends JpaRepository<JE_JE_SR_JA_ME_Paciente, Long> {

    Optional<JE_JE_SR_JA_ME_Paciente> findByDni(String dni);

    long countByActivoTrue();

    @Query("""
            select p from Paciente p
            where p.activo = :activo and (
                  :texto = ''
               or lower(p.nombres) like lower(concat('%', :texto, '%'))
               or lower(p.apellidos) like lower(concat('%', :texto, '%'))
               or lower(concat(p.nombres, ' ', p.apellidos)) like lower(concat('%', :texto, '%'))
               or p.dni like concat('%', :texto, '%'))
            order by p.apellidos, p.nombres
            """)
    List<JE_JE_SR_JA_ME_Paciente> JE_JE_SR_JA_ME_buscar(@Param("texto") String texto, @Param("activo") boolean activo);
}
