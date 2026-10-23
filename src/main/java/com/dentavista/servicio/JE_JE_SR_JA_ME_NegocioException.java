package com.dentavista.servicio;

/**
 * Regla de negocio incumplida. El mensaje está pensado para mostrarse tal cual
 * al usuario: dice qué pasó y, cuando se puede, qué hacer.
 */
public class JE_JE_SR_JA_ME_NegocioException extends RuntimeException {

    public JE_JE_SR_JA_ME_NegocioException(String mensaje) {
        super(mensaje);
    }
}
