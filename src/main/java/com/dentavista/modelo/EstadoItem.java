package com.dentavista.modelo;

/** Ciclo de vida de un tratamiento del plan. */
public enum EstadoItem {

    PENDIENTE("Pendiente"),
    REALIZADO("Realizado"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoItem(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
