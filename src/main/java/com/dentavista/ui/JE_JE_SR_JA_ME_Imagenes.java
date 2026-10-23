package com.dentavista.ui;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
/**
 * Conversion de imagenes de JavaFX a PNG sin depender del modulo javafx-swing:
 * se copian los pixeles a un {@link BufferedImage} y se codifican con ImageIO.
 */
public final class JE_JE_SR_JA_ME_Imagenes {

    private JE_JE_SR_JA_ME_Imagenes() {
    }

    public static byte[] JE_JE_SR_JA_ME_aPng(Image imagen) {
        int w = (int) imagen.getWidth();
        int h = (int) imagen.getHeight();
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader lector = imagen.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bi.setRGB(x, y, lector.getArgb(x, y));
            }
        }
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            ImageIO.write(bi, "png", salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo codificar la imagen", e);
        }
    }

}
