package com.dentavista.modelo;

/** Ciclo de vida de un tratamiento del plan. */
public enum JE_JE_SR_JA_ME_EstadoItem {

    PENDIENTE("Por realizar"),
    REALIZADO("Realizado"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    JE_JE_SR_JA_ME_EstadoItem(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String JE_JE_SR_JA_ME_getEtiqueta() {
        return etiqueta;
    }
}
