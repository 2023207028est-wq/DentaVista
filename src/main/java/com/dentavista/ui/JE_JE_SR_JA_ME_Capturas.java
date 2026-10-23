package com.dentavista.ui;

import com.dentavista.dominio.JE_JE_SR_JA_ME_Odontograma;
import com.dentavista.tresd.JE_JE_SR_JA_ME_VisorDental3D;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * Imágenes del odontograma para el PDF. Se dibujan fuera de pantalla en una escena
 * temporal, así no dependen de la pestaña que el usuario tenga abierta.
 */
public final class JE_JE_SR_JA_ME_Capturas {

    private JE_JE_SR_JA_ME_Capturas() {
    }

    public static Image JE_JE_SR_JA_ME_vista3D(JE_JE_SR_JA_ME_Odontograma o, JE_JE_SR_JA_ME_VisorDental3D.JE_JE_SR_JA_ME_Vista vista, double ancho, double alto) {
        JE_JE_SR_JA_ME_VisorDental3D visor = new JE_JE_SR_JA_ME_VisorDental3D();
        visor.JE_JE_SR_JA_ME_numerosVisiblesProperty().set(false);
        visor.JE_JE_SR_JA_ME_setOdontograma(o);
        visor.JE_JE_SR_JA_ME_ponerVista(vista);
        StackPane fondo = new StackPane(visor);
        fondo.setStyle("-fx-background-color: radial-gradient(center 50% 45%, radius 80%, #25344f 0%, #0e1625 75%);");
        Scene escena = new Scene(fondo, ancho, alto, true, SceneAntialiasing.BALANCED);
        fondo.applyCss();
        fondo.layout();
        return escena.snapshot(null);
    }

    public static Image JE_JE_SR_JA_ME_vista2D(JE_JE_SR_JA_ME_Odontograma o, double ancho, double alto) {
        JE_JE_SR_JA_ME_Odontograma2D o2d = new JE_JE_SR_JA_ME_Odontograma2D();
        o2d.JE_JE_SR_JA_ME_setInteractivo(false);
        o2d.JE_JE_SR_JA_ME_setOdontograma(o);
        StackPane fondo = new StackPane(o2d);
        fondo.setStyle("-fx-background-color: white; -fx-padding: 6;");
        Scene escena = new Scene(fondo, ancho, alto, Color.WHITE);
        escena.getStylesheets().add(Objects.requireNonNull(JE_JE_SR_JA_ME_Capturas.class.getResource("/css/app.css")).toExternalForm());
        fondo.applyCss();
        fondo.layout();
        return escena.snapshot(null);
    }
}
