package com.dentavista.repositorio;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Cita;
import com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface JE_JE_SR_JA_ME_CitaRepository extends JpaRepository<JE_JE_SR_JA_ME_Cita, Long> {

    List<JE_JE_SR_JA_ME_Cita> findByInicioBetweenOrderByInicioAsc(LocalDateTime desde, LocalDateTime hasta);

    List<JE_JE_SR_JA_ME_Cita> findByOdontologoAndInicioBetweenOrderByInicioAsc(JE_JE_SR_JA_ME_Usuario odontologo, LocalDateTime desde, LocalDateTime hasta);

    List<JE_JE_SR_JA_ME_Cita> findByPacienteOrderByInicioDesc(JE_JE_SR_JA_ME_Paciente paciente);

    List<JE_JE_SR_JA_ME_Cita> findByPacienteAndInicioBetween(JE_JE_SR_JA_ME_Paciente paciente, LocalDateTime desde, LocalDateTime hasta);

    long countByEstado(JE_JE_SR_JA_ME_EstadoCita estado);
}
