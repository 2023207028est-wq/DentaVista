package com.dentavista.modelo;

/** Medios de pago habituales en un consultorio peruano. */
public enum JE_JE_SR_JA_ME_MedioPago {

    EFECTIVO("Efectivo"),
    YAPE("Yape"),
    PLIN("Plin"),
    TARJETA("Tarjeta"),
    TRANSFERENCIA("Transferencia");

    private final String etiqueta;

    JE_JE_SR_JA_ME_MedioPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String JE_JE_SR_JA_ME_getEtiqueta() {
        return etiqueta;
    }
}
