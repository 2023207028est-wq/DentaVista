package com.dentavista.ui;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Parent;
import org.springframework.stereotype.Component;

import java.util.prefs.Preferences;

/** Tema claro / oscuro. La elección se recuerda entre sesiones. */
@Component
public class JE_JE_SR_JA_ME_Tema {

    private static final String CLAVE = "tema-oscuro";
    private final Preferences prefs = Preferences.userNodeForPackage(JE_JE_SR_JA_ME_Tema.class);
    private final BooleanProperty oscuro = new SimpleBooleanProperty(false);
    private Parent raiz;

    public void JE_JE_SR_JA_ME_aplicarInicial() {
        oscuro.set(prefs.getBoolean(CLAVE, false));
        JE_JE_SR_JA_ME_aplicar();
        oscuro.addListener((o, a, b) -> {
            prefs.putBoolean(CLAVE, b);
            JE_JE_SR_JA_ME_aplicar();
        });
    }

    public void JE_JE_SR_JA_ME_registrar(Parent raiz) {
        this.raiz = raiz;
        if (!raiz.getStyleClass().contains("app")) {
            raiz.getStyleClass().add("app");
        }
        JE_JE_SR_JA_ME_aplicar();
    }

    public BooleanProperty JE_JE_SR_JA_ME_oscuroProperty() {
        return oscuro;
    }

    public void JE_JE_SR_JA_ME_alternar() {
        oscuro.set(!oscuro.get());
    }

    private void JE_JE_SR_JA_ME_aplicar() {
        Application.setUserAgentStylesheet(oscuro.get()
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
        if (raiz != null) {
            raiz.getStyleClass().remove("dark");
            if (oscuro.get()) {
                raiz.getStyleClass().add("dark");
            }
        }
    }
}
