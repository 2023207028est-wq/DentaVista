package com.dentavista.ui;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import com.dentavista.servicio.JE_JE_SR_JA_ME_NegocioException;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

/**
 * Retroalimentación visual: notificaciones flotantes, confirmaciones y diálogos con
 * formulario. Los errores de negocio de un formulario se muestran <b>dentro</b> del
 * diálogo, sin cerrarlo, para no perder lo que el usuario escribió.
 */
@Slf4j
@Component
public class JE_JE_SR_JA_ME_Notificador {

    /** Acción de un diálogo: si lanza {@link JE_JE_SR_JA_ME_NegocioException} el diálogo sigue abierto. */
    @FunctionalInterface
    public interface JE_JE_SR_JA_ME_Accion {
        void JE_JE_SR_JA_ME_ejecutar();
    }

    private ModalPane modal;
    private VBox zonaToasts;

    public void JE_JE_SR_JA_ME_registrar(ModalPane modal, VBox zonaToasts) {
        this.modal = modal;
        this.zonaToasts = zonaToasts;
    }

    // ------------------------------------------------------------- toasts

    public void JE_JE_SR_JA_ME_exito(String mensaje) {
        JE_JE_SR_JA_ME_toast(mensaje, Feather.CHECK_CIRCLE, Styles.SUCCESS);
    }

    public void JE_JE_SR_JA_ME_info(String mensaje) {
        JE_JE_SR_JA_ME_toast(mensaje, Feather.INFO, Styles.ACCENT);
    }

    public void JE_JE_SR_JA_ME_advertencia(String mensaje) {
        JE_JE_SR_JA_ME_toast(mensaje, Feather.ALERT_TRIANGLE, Styles.WARNING);
    }

    public void JE_JE_SR_JA_ME_error(String mensaje) {
        JE_JE_SR_JA_ME_toast(mensaje, Feather.X_CIRCLE, Styles.DANGER);
    }

    /** Ejecuta una acción y convierte cualquier fallo en un toast de error. */
    public boolean JE_JE_SR_JA_ME_intentar(JE_JE_SR_JA_ME_Accion accion) {
        try {
            accion.JE_JE_SR_JA_ME_ejecutar();
            return true;
        } catch (JE_JE_SR_JA_ME_NegocioException e) {
            JE_JE_SR_JA_ME_advertencia(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error inesperado", e);
            JE_JE_SR_JA_ME_error("Ocurrió un error inesperado: " + e.getMessage());
        }
        return false;
    }

    private void JE_JE_SR_JA_ME_toast(String mensaje, Ikon icono, String estilo) {
        if (zonaToasts == null) {
            return;
        }
        Notification n = new Notification(mensaje, new FontIcon(icono));
        n.getStyleClass().addAll(estilo, Styles.ELEVATED_2, "toast");
        n.setPrefWidth(380);
        n.setMaxWidth(380);
        n.setOnClose(e -> JE_JE_SR_JA_ME_cerrarToast(n));
        zonaToasts.getChildren().add(n);
        Animations.fadeInUp(n, Duration.millis(260)).play();
        PauseTransition espera = new PauseTransition(Duration.seconds(3.8));
        espera.setOnFinished(e -> JE_JE_SR_JA_ME_cerrarToast(n));
        espera.play();
    }

    private void JE_JE_SR_JA_ME_cerrarToast(Notification n) {
        if (!zonaToasts.getChildren().contains(n) || n.getProperties().containsKey("cerrando")) {
            return;
        }
        n.getProperties().put("cerrando", true);
        var salida = Animations.fadeOutRight(n, Duration.millis(220));
        salida.setOnFinished(e -> zonaToasts.getChildren().remove(n));
        salida.play();
    }

    // ------------------------------------------------------------- diálogos

    /** Confirmación simple; {@code peligro} pinta el botón de rojo. */
    public void JE_JE_SR_JA_ME_confirmar(String titulo, String mensaje, String textoAccion, boolean peligro, Runnable accion) {
        Label texto = new Label(mensaje);
        texto.setWrapText(true);
        texto.getStyleClass().add("texto-tenue");
        JE_JE_SR_JA_ME_dialogo(peligro ? Feather.ALERT_TRIANGLE : Feather.HELP_CIRCLE, titulo, texto, textoAccion, peligro,
                accion::run, 440);
    }

    /**
     * Diálogo modal con contenido propio. El botón principal ejecuta {@code accion};
     * si termina bien el diálogo se cierra, si lanza {@link JE_JE_SR_JA_ME_NegocioException} el error
     * aparece dentro del diálogo.
     */
    public void JE_JE_SR_JA_ME_dialogo(Ikon icono, String titulo, Node contenido, String textoAccion, boolean peligro,
                        JE_JE_SR_JA_ME_Accion accion, double ancho) {
        FontIcon ic = new FontIcon(icono);
        ic.setIconSize(20);
        // setIconColor y no setStyle: un estilo en línea pisa la fuente de iconos de Ikonli.
        ic.setIconColor(javafx.scene.paint.Color.web(peligro ? "#dc2626" : "#0d9488"));
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("dialogo-titulo");
        HBox cab = new HBox(10, ic, lblTitulo);
        cab.setAlignment(Pos.CENTER_LEFT);

        Label error = new Label();
        error.getStyleClass().add("dialogo-error");
        error.setMaxWidth(Double.MAX_VALUE);
        error.setVisible(false);
        error.setManaged(false);

        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().addAll("secundario", Styles.BUTTON_OUTLINED);
        cancelar.setCancelButton(true);
        cancelar.setOnAction(e -> JE_JE_SR_JA_ME_cerrar());

        Button aceptar = new Button(textoAccion);
        aceptar.getStyleClass().add(peligro ? Styles.DANGER : "primario");
        aceptar.setDefaultButton(!(contenido instanceof javafx.scene.control.TextArea));
        aceptar.setOnAction(e -> {
            try {
                accion.JE_JE_SR_JA_ME_ejecutar();
                JE_JE_SR_JA_ME_cerrar();
            } catch (JE_JE_SR_JA_ME_NegocioException ex) {
                error.setText(ex.getMessage());
                error.setVisible(true);
                error.setManaged(true);
                Animations.shakeX(error).play();
            } catch (RuntimeException ex) {
                log.error("Error en diálogo", ex);
                error.setText("Error inesperado: " + ex.getMessage());
                error.setVisible(true);
                error.setManaged(true);
            }
        });

        Region espacio = new Region();
        HBox.setHgrow(espacio, javafx.scene.layout.Priority.ALWAYS);
        HBox acciones = new HBox(10, espacio, cancelar, aceptar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        VBox caja = new VBox(16, cab, contenido, error, acciones);
        caja.getStyleClass().add("dialogo");
        caja.setMaxSize(ancho, Region.USE_PREF_SIZE);
        caja.setPrefWidth(ancho);
        JE_JE_SR_JA_ME_mostrar(caja);
    }

    /** Muestra cualquier nodo como capa modal (se cierra con {@link #JE_JE_SR_JA_ME_cerrar()}). */
    public void JE_JE_SR_JA_ME_mostrar(Node nodo) {
        modal.usePredefinedTransitionFactories(null);
        modal.show(nodo);
    }

    public void JE_JE_SR_JA_ME_cerrar() {
        modal.hide(true);
    }
}
