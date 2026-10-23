package com.dentavista.modelo;

import com.dentavista.dominio.JE_JE_SR_JA_ME_Pieza;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Una línea del plan de tratamiento (y del presupuesto) de un paciente. */
@Entity(name = "ItemPlan")
@Table(name = "item_plan", indexes = @Index(name = "ix_item_paciente", columnList = "paciente_id, estado"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"id", "pieza", "superficie", "estado"})
@EqualsAndHashCode(of = "id")
public class JE_JE_SR_JA_ME_ItemPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private JE_JE_SR_JA_ME_Paciente paciente;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "tratamiento_id", nullable = false)
    private JE_JE_SR_JA_ME_Tratamiento tratamiento;

    /** Pieza FDI; null en procedimientos generales. */
    private Integer pieza;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private JE_JE_SR_JA_ME_Superficie superficie;

    /** Precio congelado al crear el ítem (el catálogo puede cambiar después). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private JE_JE_SR_JA_ME_EstadoItem estado = JE_JE_SR_JA_ME_EstadoItem.PENDIENTE;

    @Column(nullable = false)
    private LocalDateTime creado;

    private LocalDateTime realizado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "consulta_id")
    private JE_JE_SR_JA_ME_Consulta consulta;

    @Size(max = 200)
    @Column(length = 200)
    private String nota;

    /** "16 · Oclusal" o "General". */
    public String JE_JE_SR_JA_ME_ubicacion() {
        if (pieza == null) {
            return "General";
        }
        String texto = String.valueOf(pieza);
        if (superficie != null && superficie.JE_JE_SR_JA_ME_esCara()) {
            texto += " · " + superficie.JE_JE_SR_JA_ME_etiqueta(pieza);
        }
        return texto;
    }

    public String JE_JE_SR_JA_ME_descripcionPieza() {
        return pieza == null ? "Procedimiento general" : JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_nombre(pieza);
    }
}
