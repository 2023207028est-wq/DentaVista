package com.dentavista;

import javafx.application.Application;

/**
 * Punto de entrada. Se separa de {@link JE_JE_SR_JA_ME_JavaFxApplication} para que el JAR
 * ejecutable arranque JavaFX sin exigir el module-path.
 */
public class JE_JE_SR_JA_ME_Launcher {

    public static void main(String[] args) {
        // Antialiasing de escena 3D y texto nítido en pantallas HiDPI.
        System.setProperty("prism.lcdtext", "false");
        Application.launch(JE_JE_SR_JA_ME_JavaFxApplication.class, args);
    }
}
