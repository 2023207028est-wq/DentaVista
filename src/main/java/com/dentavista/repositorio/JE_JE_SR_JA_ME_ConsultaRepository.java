package com.dentavista.repositorio;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Consulta;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JE_JE_SR_JA_ME_ConsultaRepository extends JpaRepository<JE_JE_SR_JA_ME_Consulta, Long> {

    List<JE_JE_SR_JA_ME_Consulta> findByPacienteOrderByFechaDesc(JE_JE_SR_JA_ME_Paciente paciente);

    List<JE_JE_SR_JA_ME_Consulta> findByPacienteOrderByFechaAsc(JE_JE_SR_JA_ME_Paciente paciente);

    Optional<JE_JE_SR_JA_ME_Consulta> findFirstByPacienteAndFechaBetweenOrderByFechaDesc(
            JE_JE_SR_JA_ME_Paciente paciente, LocalDateTime desde, LocalDateTime hasta);

    Optional<JE_JE_SR_JA_ME_Consulta> findFirstByPacienteOrderByFechaDesc(JE_JE_SR_JA_ME_Paciente paciente);

    long countByFechaBetween(LocalDateTime desde, LocalDateTime hasta);

    List<JE_JE_SR_JA_ME_Consulta> findTop8ByOrderByFechaDesc();
}
