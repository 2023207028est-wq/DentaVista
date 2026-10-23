package com.dentavista.servicio;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Consulta;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_ConsultaRepository;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Indicadores de la pantalla de inicio (los de dinero vienen de {@link JE_JE_SR_JA_ME_PagoService}). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JE_JE_SR_JA_ME_EstadisticaService {

    public record JE_JE_SR_JA_ME_Resumen(long pacientes, long consultasMes) {
    }

    private final JE_JE_SR_JA_ME_PacienteRepository pacientes;
    private final JE_JE_SR_JA_ME_ConsultaRepository consultas;

    public JE_JE_SR_JA_ME_Resumen JE_JE_SR_JA_ME_resumen() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes = hoy.plusMonths(1).withDayOfMonth(1).atStartOfDay().minusNanos(1);
        return new JE_JE_SR_JA_ME_Resumen(pacientes.countByActivoTrue(), consultas.countByFechaBetween(inicioMes, finMes));
    }

    public List<JE_JE_SR_JA_ME_Consulta> JE_JE_SR_JA_ME_ultimasConsultas() {
        return consultas.findTop8ByOrderByFechaDesc();
    }
}
