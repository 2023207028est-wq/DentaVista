package com.dentavista.servicio;

import com.dentavista.dominio.EstadoDental;
import com.dentavista.modelo.Consulta;
import com.dentavista.modelo.EstadoItem;
import com.dentavista.repositorio.ConsultaRepository;
import com.dentavista.repositorio.HallazgoRepository;
import com.dentavista.repositorio.ItemPlanRepository;
import com.dentavista.repositorio.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Indicadores de la pantalla de inicio. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstadisticaService {

    public record Resumen(long pacientes, long consultasMes, BigDecimal porRealizar,
                          BigDecimal facturadoMes, long tratamientosMes) {
    }

    private final PacienteRepository pacientes;
    private final ConsultaRepository consultas;
    private final ItemPlanRepository items;
    private final HallazgoRepository hallazgos;

    public Resumen resumen() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes = hoy.plusMonths(1).withDayOfMonth(1).atStartOfDay().minusNanos(1);
        return new Resumen(
                pacientes.countByActivoTrue(),
                consultas.countByFechaBetween(inicioMes, finMes),
                items.sumarPorEstado(EstadoItem.PENDIENTE),
                items.sumarRealizadoEntre(inicioMes, finMes),
                items.contarRealizadoEntre(inicioMes, finMes));
    }

    public List<Consulta> ultimasConsultas() {
        return consultas.findTop8ByOrderByFechaDesc();
    }
}
