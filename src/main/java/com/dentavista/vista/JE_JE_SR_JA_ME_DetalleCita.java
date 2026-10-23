package com.dentavista.vista;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Cita;
import com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita;
import com.dentavista.servicio.JE_JE_SR_JA_ME_CitaService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_SesionService;
import com.dentavista.ui.JE_JE_SR_JA_ME_Formato;
import com.dentavista.ui.JE_JE_SR_JA_ME_Notificador;
import com.dentavista.ui.JE_JE_SR_JA_ME_Ui;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

/** Detalle de una cita con sus acciones (confirmar, atender, no asistió, cancelar). */
@Component
@RequiredArgsConstructor
public class JE_JE_SR_JA_ME_DetalleCita {

    private final JE_JE_SR_JA_ME_CitaService citas;
    private final JE_JE_SR_JA_ME_SesionService sesion;
    private final JE_JE_SR_JA_ME_Notificador notificador;
    private final JE_JE_SR_JA_ME_Navegador navegador;
    private final com.dentavista.servicio.JE_JE_SR_JA_ME_DatosClinica clinica;
    private final JE_JE_SR_JA_ME_DialogoCita dialogoCita;

    public void JE_JE_SR_JA_ME_abrir(JE_JE_SR_JA_ME_Cita c, Runnable alCambiar) {
        Label titulo = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(c.getPaciente().JE_JE_SR_JA_ME_nombreCompleto(), "dialogo-titulo");
        Label estado = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip(c.getEstado().JE_JE_SR_JA_ME_getEtiqueta(), JE_JE_SR_JA_ME_claseEstado(c.getEstado()), null);
        HBox cab = new HBox(10, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_avatar(c.getPaciente().JE_JE_SR_JA_ME_iniciales(), c.getPaciente().getDni(), 40),
                new VBox(2, titulo, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("DNI " + c.getPaciente().getDni()
                        + (c.getPaciente().getTelefono() != null ? " · " + c.getPaciente().getTelefono() : ""), "texto-tenue")),
                JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_espaciador(), estado);
        cab.setAlignment(Pos.CENTER_LEFT);

        VBox datos = new VBox(8,
                JE_JE_SR_JA_ME_fila(Feather.CALENDAR, JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_fechaLarga(c.getInicio()) + " · " + c.getInicio().toLocalTime()
                        + " a " + c.JE_JE_SR_JA_ME_fin().toLocalTime()),
                JE_JE_SR_JA_ME_fila(Feather.USER, c.getOdontologo().getNombre()),
                JE_JE_SR_JA_ME_fila(Feather.CLIPBOARD, c.JE_JE_SR_JA_ME_descripcion()));
        if (c.getMotivo() != null && c.getItemPlan() != null) {
            datos.getChildren().add(JE_JE_SR_JA_ME_fila(Feather.MESSAGE_SQUARE, c.getMotivo()));
        }
        if (c.getPaciente().getAlergias() != null) {
            datos.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Alergia: " + c.getPaciente().getAlergias(), "peligro", Feather.ALERT_OCTAGON));
        }
        datos.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Registrada por " + c.getCreadaPor() + " el " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_fechaHora(c.getCreadaEn()),
                "texto-tenue", "texto-pequeno"));

        FlowPane acciones = new FlowPane(8, 8);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        Button cerrar = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("Cerrar", Feather.X);
        cerrar.setCancelButton(true);
        cerrar.setOnAction(e -> notificador.JE_JE_SR_JA_ME_cerrar());
        if (sesion.JE_JE_SR_JA_ME_esPersonal()) {
            Button ficha = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("Abrir ficha", Feather.BOX);
            ficha.setOnAction(e -> {
                notificador.JE_JE_SR_JA_ME_cerrar();
                navegador.JE_JE_SR_JA_ME_abrirFicha(c.getPaciente());
            });
            acciones.getChildren().add(ficha);
        }
        if (c.getEstado().JE_JE_SR_JA_ME_abierta() && sesion.JE_JE_SR_JA_ME_esPersonal()) {
            Button whatsapp = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("Recordar por WhatsApp", Feather.MESSAGE_CIRCLE);
            String numero = JE_JE_SR_JA_ME_numeroWhatsApp(c.getPaciente().getTelefono());
            whatsapp.setDisable(numero == null);
            JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(whatsapp, numero == null ? "El paciente no tiene un celular registrado"
                    : "Abre WhatsApp con el recordatorio listo para enviar a " + c.getPaciente().getTelefono());
            whatsapp.setOnAction(e -> com.dentavista.JE_JE_SR_JA_ME_JavaFxApplication.JE_JE_SR_JA_ME_get().JE_JE_SR_JA_ME_hostServices().showDocument(
                    "https://wa.me/" + numero + "?text=" + java.net.URLEncoder.encode(JE_JE_SR_JA_ME_mensaje(c),
                            java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20")));
            acciones.getChildren().add(whatsapp);
        }
        if (c.getEstado().JE_JE_SR_JA_ME_abierta()) {
            if (sesion.JE_JE_SR_JA_ME_esPersonal()) {
                Button noAsistio = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("No asistió", Feather.USER_X);
                noAsistio.setOnAction(e -> JE_JE_SR_JA_ME_accion(() -> citas.JE_JE_SR_JA_ME_marcarNoAsistio(c), "Marcada como no asistió.", alCambiar));
                acciones.getChildren().add(noAsistio);
            }
            Button cancelar = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("Cancelar cita", Feather.X_CIRCLE);
            cancelar.setOnAction(e -> JE_JE_SR_JA_ME_accion(() -> citas.JE_JE_SR_JA_ME_cancelar(c), "Cita cancelada: el horario quedó libre.", alCambiar));
            Button reprogramar = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("Reprogramar", Feather.REPEAT);
            JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(reprogramar, "Cambiar fecha, hora u odontólogo");
            reprogramar.setOnAction(e -> {
                notificador.JE_JE_SR_JA_ME_cerrar();
                dialogoCita.JE_JE_SR_JA_ME_reprogramar(c, x -> alCambiar.run());
            });
            acciones.getChildren().addAll(cancelar, reprogramar);
            if (sesion.JE_JE_SR_JA_ME_esPersonal() && c.getEstado() == JE_JE_SR_JA_ME_EstadoCita.SOLICITADA) {
                Button confirmar = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_primario("Confirmar", Feather.CHECK);
                confirmar.setOnAction(e -> JE_JE_SR_JA_ME_accion(() -> citas.JE_JE_SR_JA_ME_confirmar(c), "Cita confirmada.", alCambiar));
                acciones.getChildren().add(confirmar);
            } else if (sesion.JE_JE_SR_JA_ME_esPersonal()) {
                Button atender = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_primario("Atender ahora", Feather.PLAY);
                atender.setOnAction(e -> {
                    if (notificador.JE_JE_SR_JA_ME_intentar(() -> citas.JE_JE_SR_JA_ME_marcarAtendida(c))) {
                        notificador.JE_JE_SR_JA_ME_cerrar();
                        alCambiar.run();
                        navegador.JE_JE_SR_JA_ME_abrirFicha(c.getPaciente());
                    }
                });
                acciones.getChildren().add(atender);
            }
        }
        acciones.getChildren().add(0, cerrar);

        VBox caja = new VBox(16, cab, datos, acciones);
        caja.getStyleClass().add("dialogo");
        caja.setMaxSize(520, Region.USE_PREF_SIZE);
        caja.setPrefWidth(520);
        notificador.JE_JE_SR_JA_ME_mostrar(caja);
    }

    /** Mensaje de recordatorio con fecha, hora, odontólogo y motivo. */
    String JE_JE_SR_JA_ME_mensaje(JE_JE_SR_JA_ME_Cita c) {
        java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern(
                "EEEE d 'de' MMMM", java.util.Locale.forLanguageTag("es-PE"));
        return "Hola " + c.getPaciente().getNombres().split(" ")[0] + ", le saludamos de " + clinica.nombre()
                + ". Le recordamos su cita el " + f.format(c.getInicio()) + " a las " + c.getInicio().toLocalTime()
                + " con " + c.getOdontologo().getNombre() + " (" + c.JE_JE_SR_JA_ME_descripcion() + "). "
                + "Por favor confirme su asistencia respondiendo este mensaje. ¡Gracias!";
    }

    /** Celular peruano en formato internacional para wa.me (51 + 9 dígitos), o null. */
    static String JE_JE_SR_JA_ME_numeroWhatsApp(String telefono) {
        if (telefono == null) {
            return null;
        }
        String d = telefono.replaceAll("\\D", "");
        if (d.startsWith("51") && d.length() == 11) {
            d = d.substring(2);
        }
        return d.length() == 9 && d.startsWith("9") ? "51" + d : null;
    }

    private void JE_JE_SR_JA_ME_accion(Runnable r, String mensaje, Runnable alCambiar) {
        if (notificador.JE_JE_SR_JA_ME_intentar(r::run)) {
            notificador.JE_JE_SR_JA_ME_cerrar();
            notificador.JE_JE_SR_JA_ME_exito(mensaje);
            alCambiar.run();
        }
    }

    private static HBox JE_JE_SR_JA_ME_fila(Feather icono, String texto) {
        Label l = new Label(texto);
        l.setWrapText(true);
        HBox h = new HBox(10, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(icono, 16), l);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    static String JE_JE_SR_JA_ME_claseEstado(JE_JE_SR_JA_ME_EstadoCita e) {
        return switch (e) {
            case SOLICITADA -> "alerta";
            case CONFIRMADA -> "info";
            case ATENDIDA -> "exito";
            case NO_ASISTIO -> "peligro";
            case CANCELADA -> "neutro";
        };
    }
}
