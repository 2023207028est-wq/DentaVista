package com.dentavista.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/** Cita de un paciente con un odontólogo, opcionalmente para un tratamiento del plan. */
@Entity(name = "Cita")
@Table(name = "cita", indexes = {
        @Index(name = "ix_cita_inicio", columnList = "inicio"),
        @Index(name = "ix_cita_paciente", columnList = "paciente_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"id", "inicio", "estado"})
@EqualsAndHashCode(of = "id")
public class JE_JE_SR_JA_ME_Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "paciente_id", nullable = false)
    private JE_JE_SR_JA_ME_Paciente paciente;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "odontologo_id", nullable = false)
    private JE_JE_SR_JA_ME_Usuario odontologo;

    @Column(nullable = false)
    private LocalDateTime inicio;

    /** Minutos (30, 60 o 90). */
    @Column(nullable = false)
    private int duracion;

    @Size(max = 120)
    @Column(length = 120)
    private String motivo;

    /** Tratamiento del plan que se hará en la cita (opcional). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_plan_id")
    private JE_JE_SR_JA_ME_ItemPlan itemPlan;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private JE_JE_SR_JA_ME_EstadoCita estado = JE_JE_SR_JA_ME_EstadoCita.CONFIRMADA;

    @Size(max = 80)
    @Column(name = "creada_por", length = 80)
    private String creadaPor;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    public LocalDateTime JE_JE_SR_JA_ME_fin() {
        return inicio.plusMinutes(duracion);
    }

    public boolean JE_JE_SR_JA_ME_seCruzaCon(LocalDateTime desde, LocalDateTime hasta) {
        return inicio.isBefore(hasta) && JE_JE_SR_JA_ME_fin().isAfter(desde);
    }

    /** "Obturación con resina · 16 · Oclusal" o el motivo escrito. */
    public String JE_JE_SR_JA_ME_descripcion() {
        if (itemPlan != null) {
            return itemPlan.getTratamiento().getNombre() + (itemPlan.getPieza() != null ? " · " + itemPlan.JE_JE_SR_JA_ME_ubicacion() : "");
        }
        return motivo == null || motivo.isBlank() ? "Consulta" : motivo;
    }
}
