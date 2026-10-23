package com.dentavista.repositorio;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface JE_JE_SR_JA_ME_PagoRepository extends JpaRepository<JE_JE_SR_JA_ME_Pago, Long> {

    List<JE_JE_SR_JA_ME_Pago> findByPacienteOrderByFechaDesc(JE_JE_SR_JA_ME_Paciente paciente);

    long countByReciboStartingWith(String prefijo);

    @Query("select coalesce(sum(p.monto), 0) from Pago p where p.paciente = :paciente and p.anulado = false")
    BigDecimal JE_JE_SR_JA_ME_pagadoPor(@Param("paciente") JE_JE_SR_JA_ME_Paciente paciente);

    @Query("select coalesce(sum(p.monto), 0) from Pago p where p.anulado = false and p.fecha between :desde and :hasta")
    BigDecimal JE_JE_SR_JA_ME_cobradoEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select count(p) from Pago p where p.anulado = false and p.fecha between :desde and :hasta")
    long JE_JE_SR_JA_ME_pagosEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** {pacienteId, total pagado} de todos los pacientes con pagos. */
    @Query("select p.paciente.id, sum(p.monto) from Pago p where p.anulado = false group by p.paciente.id")
    List<Object[]> JE_JE_SR_JA_ME_pagadoPorPaciente();
}
