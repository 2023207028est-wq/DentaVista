package com.dentavista.dominio;

/**
 * Caras de una pieza dental. {@link #PIEZA} representa la pieza completa
 * (ausente, corona, implante...), no una cara concreta.
 */
public enum JE_JE_SR_JA_ME_Superficie {

    OCLUSAL("O"),
    VESTIBULAR("V"),
    LINGUAL("L"),
    MESIAL("M"),
    DISTAL("D"),
    PIEZA("");

    /** Las cinco caras clinicas, sin la pieza completa. */
    public static final JE_JE_SR_JA_ME_Superficie[] CARAS = {OCLUSAL, VESTIBULAR, LINGUAL, MESIAL, DISTAL};

    private final String sigla;

    JE_JE_SR_JA_ME_Superficie(String sigla) {
        this.sigla = sigla;
    }

    public boolean JE_JE_SR_JA_ME_esCara() {
        return this != PIEZA;
    }

    /**
     * Nombre clinico segun la pieza: en anteriores la cara oclusal es el borde
     * <i>incisal</i>; en la arcada superior la cara lingual se llama <i>palatina</i>.
     */
    public String JE_JE_SR_JA_ME_etiqueta(int fdi) {
        return switch (this) {
            case OCLUSAL -> JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_esAnterior(fdi) ? "Incisal" : "Oclusal";
            case VESTIBULAR -> "Vestibular";
            case LINGUAL -> JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_esSuperior(fdi) ? "Palatina" : "Lingual";
            case MESIAL -> "Mesial";
            case DISTAL -> "Distal";
            case PIEZA -> "Pieza completa";
        };
    }

    /** Sigla clinica (O, I, V, P, L, M, D). */
    public String JE_JE_SR_JA_ME_sigla(int fdi) {
        return switch (this) {
            case OCLUSAL -> JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_esAnterior(fdi) ? "I" : "O";
            case LINGUAL -> JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_esSuperior(fdi) ? "P" : "L";
            default -> sigla;
        };
    }
}
