package com.dentavista.modelo;

/** Ciclo de vida de una cita. */
public enum JE_JE_SR_JA_ME_EstadoCita {

    SOLICITADA("Por confirmar", "#f59e0b"),
    CONFIRMADA("Confirmada", "#0d9488"),
    ATENDIDA("Atendida", "#2563eb"),
    NO_ASISTIO("No asistió", "#64748b"),
    CANCELADA("Cancelada", "#94a3b8");

    private final String etiqueta;
    private final String color;

    JE_JE_SR_JA_ME_EstadoCita(String etiqueta, String color) {
        this.etiqueta = etiqueta;
        this.color = color;
    }

    public String JE_JE_SR_JA_ME_getEtiqueta() {
        return etiqueta;
    }

    public String JE_JE_SR_JA_ME_getColor() {
        return color;
    }

    /** Ocupa el horario (las canceladas liberan el espacio). */
    public boolean JE_JE_SR_JA_ME_ocupaHorario() {
        return this != CANCELADA;
    }

    /** Todavía puede cambiar (confirmarse, cancelarse, atenderse). */
    public boolean JE_JE_SR_JA_ME_abierta() {
        return this == SOLICITADA || this == CONFIRMADA;
    }
}
