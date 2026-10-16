package com.dentavista.repositorio;

import com.dentavista.dominio.EstadoDental;
import com.dentavista.modelo.Consulta;
import com.dentavista.modelo.Hallazgo;
import com.dentavista.modelo.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HallazgoRepository extends JpaRepository<Hallazgo, Long> {

    List<Hallazgo> findByPacienteOrderByFechaAscIdAsc(Paciente paciente);

    List<Hallazgo> findByPacienteAndPiezaOrderByFechaDescIdDesc(Paciente paciente, int pieza);

    List<Hallazgo> findByConsultaOrderByIdAsc(Consulta consulta);

    Optional<Hallazgo> findFirstByPacienteAndConsultaOrderByIdDesc(Paciente paciente, Consulta consulta);

}
