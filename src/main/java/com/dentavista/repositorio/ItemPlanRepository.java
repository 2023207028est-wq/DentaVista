package com.dentavista.repositorio;

import com.dentavista.modelo.EstadoItem;
import com.dentavista.modelo.ItemPlan;
import com.dentavista.modelo.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ItemPlanRepository extends JpaRepository<ItemPlan, Long> {

    List<ItemPlan> findByPacienteOrderByCreadoAscIdAsc(Paciente paciente);

    List<ItemPlan> findByPacienteAndEstado(Paciente paciente, EstadoItem estado);

    boolean existsByConsulta(com.dentavista.modelo.Consulta consulta);

    /** {pacienteId, cantidad, total} de los tratamientos pendientes por paciente. */
    @Query("""
            select i.paciente.id, count(i), sum(i.precio) from ItemPlan i
            where i.estado = com.dentavista.modelo.EstadoItem.PENDIENTE
            group by i.paciente.id
            """)
    List<Object[]> pendientesPorPaciente();

    /** {pacienteId, total realizado} de todos los pacientes. */
    @Query("""
            select i.paciente.id, sum(i.precio) from ItemPlan i
            where i.estado = com.dentavista.modelo.EstadoItem.REALIZADO
            group by i.paciente.id
            """)
    List<Object[]> realizadoPorPaciente();

    @Query("select coalesce(sum(i.precio), 0) from ItemPlan i where i.estado = :estado")
    BigDecimal sumarPorEstado(@Param("estado") EstadoItem estado);

    @Query("""
            select coalesce(sum(i.precio), 0) from ItemPlan i
            where i.estado = com.dentavista.modelo.EstadoItem.REALIZADO
              and i.realizado between :desde and :hasta
            """)
    BigDecimal sumarRealizadoEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("""
            select count(i) from ItemPlan i
            where i.estado = com.dentavista.modelo.EstadoItem.REALIZADO
              and i.realizado between :desde and :hasta
            """)
    long contarRealizadoEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
