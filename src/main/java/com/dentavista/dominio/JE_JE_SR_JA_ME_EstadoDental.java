package com.dentavista.dominio;

/**
 * Hallazgos clinicos que se registran sobre el odontograma.
 * <p>
 * Cada estado sabe si se aplica a una <b>cara</b> o a la <b>pieza completa</b>, que color
 * lo representa (el mismo en el modelo 3D, el odontograma 2D y el PDF) y, si es una
 * patologia, que tratamiento del catalogo la resuelve.
 * </p>
 */
public enum JE_JE_SR_JA_ME_EstadoDental {

    SANO("Sano", "#EFE8D8", JE_JE_SR_JA_ME_Alcance.AMBOS, null),
    CARIES("Caries", "#E5484D", JE_JE_SR_JA_ME_Alcance.CARA, "OBT"),
    FRACTURA("Fractura", "#F59E0B", JE_JE_SR_JA_ME_Alcance.CARA, "REC"),
    OBTURADO("Obturación (resina)", "#3B82F6", JE_JE_SR_JA_ME_Alcance.CARA, null),
    SELLANTE("Sellante", "#14B8A6", JE_JE_SR_JA_ME_Alcance.CARA, null),
    ENDODONCIA_INDICADA("Endodoncia indicada", "#C026D3", JE_JE_SR_JA_ME_Alcance.PIEZA, "END"),
    ENDODONCIA("Endodoncia realizada", "#8B5CF6", JE_JE_SR_JA_ME_Alcance.PIEZA, null),
    EXTRACCION_INDICADA("Extracción indicada", "#B91C1C", JE_JE_SR_JA_ME_Alcance.PIEZA, "EXO"),
    CORONA("Corona", "#D4A72C", JE_JE_SR_JA_ME_Alcance.PIEZA, null),
    IMPLANTE("Implante", "#7C8DA6", JE_JE_SR_JA_ME_Alcance.PIEZA, null),
    AUSENTE("Ausente", "#64748B", JE_JE_SR_JA_ME_Alcance.PIEZA, null);

    /** A que se aplica el estado. */
    public enum JE_JE_SR_JA_ME_Alcance { CARA, PIEZA, AMBOS }

    private final String etiqueta;
    private final String color;
    private final JE_JE_SR_JA_ME_Alcance alcance;
    private final String tratamientoSugerido;

    JE_JE_SR_JA_ME_EstadoDental(String etiqueta, String color, JE_JE_SR_JA_ME_Alcance alcance, String tratamientoSugerido) {
        this.etiqueta = etiqueta;
        this.color = color;
        this.alcance = alcance;
        this.tratamientoSugerido = tratamientoSugerido;
    }

    public String JE_JE_SR_JA_ME_getEtiqueta() {
        return etiqueta;
    }

    /** Color hexadecimal (#RRGGBB). */
    public String JE_JE_SR_JA_ME_getColor() {
        return color;
    }

    public JE_JE_SR_JA_ME_Alcance JE_JE_SR_JA_ME_getAlcance() {
        return alcance;
    }

    public boolean JE_JE_SR_JA_ME_seAplicaACara() {
        return alcance != JE_JE_SR_JA_ME_Alcance.PIEZA;
    }

    public boolean JE_JE_SR_JA_ME_seAplicaAPieza() {
        return alcance != JE_JE_SR_JA_ME_Alcance.CARA;
    }

    /** Codigo del tratamiento del catalogo que corrige esta patologia (o null). */
    public String JE_JE_SR_JA_ME_getTratamientoSugerido() {
        return tratamientoSugerido;
    }

    /** Requiere tratamiento: entra al plan y al presupuesto. */
    public boolean JE_JE_SR_JA_ME_esPatologia() {
        return tratamientoSugerido != null;
    }

    /**
     * La pieza ya no tiene caras propias que tratar: ausente, implante o corona
     * la cubren por completo.
     */
    public boolean JE_JE_SR_JA_ME_anulaCaras() {
        return this == AUSENTE || this == IMPLANTE || this == CORONA;
    }
}
