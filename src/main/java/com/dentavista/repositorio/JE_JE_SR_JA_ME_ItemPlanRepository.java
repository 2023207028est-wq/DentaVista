package com.dentavista.repositorio;

import com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoItem;
import com.dentavista.modelo.JE_JE_SR_JA_ME_ItemPlan;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface JE_JE_SR_JA_ME_ItemPlanRepository extends JpaRepository<JE_JE_SR_JA_ME_ItemPlan, Long> {

    List<JE_JE_SR_JA_ME_ItemPlan> findByPacienteOrderByCreadoAscIdAsc(JE_JE_SR_JA_ME_Paciente paciente);

    List<JE_JE_SR_JA_ME_ItemPlan> findByPacienteAndEstado(JE_JE_SR_JA_ME_Paciente paciente, JE_JE_SR_JA_ME_EstadoItem estado);

    boolean existsByConsulta(com.dentavista.modelo.JE_JE_SR_JA_ME_Consulta consulta);

    /** {pacienteId, cantidad, total} de los tratamientos pendientes por paciente. */
    @Query("""
            select i.paciente.id, count(i), sum(i.precio) from ItemPlan i
            where i.estado = com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoItem.PENDIENTE
            group by i.paciente.id
            """)
    List<Object[]> JE_JE_SR_JA_ME_pendientesPorPaciente();

    /** {pacienteId, total realizado} de todos los pacientes. */
    @Query("""
            select i.paciente.id, sum(i.precio) from ItemPlan i
            where i.estado = com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoItem.REALIZADO
            group by i.paciente.id
            """)
    List<Object[]> JE_JE_SR_JA_ME_realizadoPorPaciente();
}
