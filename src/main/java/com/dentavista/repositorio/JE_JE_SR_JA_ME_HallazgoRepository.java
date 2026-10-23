package com.dentavista.repositorio;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Consulta;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Hallazgo;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JE_JE_SR_JA_ME_HallazgoRepository extends JpaRepository<JE_JE_SR_JA_ME_Hallazgo, Long> {

    List<JE_JE_SR_JA_ME_Hallazgo> findByPacienteOrderByFechaAscIdAsc(JE_JE_SR_JA_ME_Paciente paciente);

    List<JE_JE_SR_JA_ME_Hallazgo> findByPacienteAndPiezaOrderByFechaDescIdDesc(JE_JE_SR_JA_ME_Paciente paciente, int pieza);

    List<JE_JE_SR_JA_ME_Hallazgo> findByConsultaOrderByIdAsc(JE_JE_SR_JA_ME_Consulta consulta);

    Optional<JE_JE_SR_JA_ME_Hallazgo> findFirstByPacienteAndConsultaOrderByIdDesc(JE_JE_SR_JA_ME_Paciente paciente, JE_JE_SR_JA_ME_Consulta consulta);

}
