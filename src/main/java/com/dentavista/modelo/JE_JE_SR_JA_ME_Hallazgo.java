package com.dentavista.modelo;

import com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Odontograma;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro clínico sobre una pieza o una de sus caras. Es la única fuente de verdad
 * del odontograma: el estado de la boca se reconstruye a partir de estos registros.
 */
@Entity(name = "Hallazgo")
@Table(name = "hallazgo", indexes = @Index(name = "ix_hallazgo_paciente", columnList = "paciente_id, fecha"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"id", "pieza", "superficie", "estado", "fecha"})
@EqualsAndHashCode(of = "id")
public class JE_JE_SR_JA_ME_Hallazgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private JE_JE_SR_JA_ME_Paciente paciente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "consulta_id")
    private JE_JE_SR_JA_ME_Consulta consulta;

    /** Pieza en notación FDI (11..48). */
    @Column(nullable = false)
    private int pieza;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private JE_JE_SR_JA_ME_Superficie superficie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private JE_JE_SR_JA_ME_EstadoDental estado;

    @Size(max = 300)
    @Column(length = 300)
    private String nota;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Size(max = 80)
    @Column(name = "registrado_por", length = 80)
    private String registradoPor;

    public JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_Registro JE_JE_SR_JA_ME_aRegistro() {
        return new JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_Registro(pieza, superficie, estado, fecha, id == null ? 0 : id);
    }
}
