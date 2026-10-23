package com.dentavista.servicio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Hash de contraseñas con SHA-256 y sal aleatoria por usuario.
 * Se guarda {@code sal:hash} para poder verificar sin conocer la contraseña.
 */
public final class JE_JE_SR_JA_ME_Seguridad {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private JE_JE_SR_JA_ME_Seguridad() {
    }

    public static String JE_JE_SR_JA_ME_hashear(String contrasena) {
        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);
        String hexSal = HexFormat.of().formatHex(sal);
        return hexSal + ":" + JE_JE_SR_JA_ME_digest(hexSal + contrasena);
    }

    public static boolean JE_JE_SR_JA_ME_coincide(String contrasena, String guardado) {
        if (guardado == null || !guardado.contains(":") || contrasena == null) {
            return false;
        }
        String sal = guardado.substring(0, guardado.indexOf(':'));
        // Comparación en tiempo constante: no filtra cuántos caracteres coinciden.
        return MessageDigest.isEqual(
                JE_JE_SR_JA_ME_digest(sal + contrasena).getBytes(StandardCharsets.UTF_8),
                guardado.substring(guardado.indexOf(':') + 1).getBytes(StandardCharsets.UTF_8));
    }

    private static String JE_JE_SR_JA_ME_digest(String texto) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
        }
    }
}
