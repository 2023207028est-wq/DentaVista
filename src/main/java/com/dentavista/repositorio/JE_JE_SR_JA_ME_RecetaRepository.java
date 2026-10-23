package com.dentavista.repositorio;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Receta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JE_JE_SR_JA_ME_RecetaRepository extends JpaRepository<JE_JE_SR_JA_ME_Receta, Long> {

    List<JE_JE_SR_JA_ME_Receta> findByPacienteOrderByFechaDesc(JE_JE_SR_JA_ME_Paciente paciente);

}
