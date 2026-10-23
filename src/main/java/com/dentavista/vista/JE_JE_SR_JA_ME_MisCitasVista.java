package com.dentavista.vista;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Cita;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.servicio.JE_JE_SR_JA_ME_CitaService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_DatosClinica;
import com.dentavista.servicio.JE_JE_SR_JA_ME_PlanService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_SesionService;
import com.dentavista.ui.JE_JE_SR_JA_ME_Ui;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Portal del paciente: su próxima cita, pedir una nueva y su historial de citas. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class JE_JE_SR_JA_ME_MisCitasVista {

    private static final Locale PE = Locale.forLanguageTag("es-PE");

    private final JE_JE_SR_JA_ME_CitaService citas;
    private final JE_JE_SR_JA_ME_PlanService planes;
    private final JE_JE_SR_JA_ME_SesionService sesion;
    private final JE_JE_SR_JA_ME_DatosClinica clinica;
    private final JE_JE_SR_JA_ME_DialogoCita dialogo;
    private final JE_JE_SR_JA_ME_DetalleCita detalle;

    private final VBox contenido = new VBox(18);

    public Node JE_JE_SR_JA_ME_vista() {
        contenido.getStyleClass().add("pagina");
        JE_JE_SR_JA_ME_dibujar();
        ScrollPane s = new ScrollPane(contenido);
        s.setFitToWidth(true);
        return s;
    }

    private void JE_JE_SR_JA_ME_dibujar() {
        JE_JE_SR_JA_ME_Paciente p = sesion.JE_JE_SR_JA_ME_pacientePropio();
        contenido.getChildren().clear();
        Label titulo = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Mis citas", "titulo-pagina");
        Label sub = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(clinica.nombre() + " · " + clinica.telefono(), "subtitulo-pagina");
        Button pedir = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_primario("Pedir una cita", Feather.CALENDAR);
        pedir.setOnAction(e -> dialogo.JE_JE_SR_JA_ME_abrir(new JE_JE_SR_JA_ME_DialogoCita.JE_JE_SR_JA_ME_Sugerencia(p, null, null, null), c -> JE_JE_SR_JA_ME_dibujar()));
        HBox cab = new HBox(12, new VBox(2, titulo, sub), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_espaciador(), pedir);
        cab.setAlignment(Pos.CENTER_LEFT);
        contenido.getChildren().add(cab);

        Optional<JE_JE_SR_JA_ME_Cita> proxima = citas.JE_JE_SR_JA_ME_proxima(p);
        if (proxima.isPresent()) {
            JE_JE_SR_JA_ME_Cita c = proxima.get();
            DateTimeFormatter f = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", PE);
            Label cuando = new Label(JE_JE_SR_JA_ME_capitalizar(f.format(c.getInicio())) + " · " + JE_JE_SR_JA_ME_AgendaVista.JE_JE_SR_JA_ME_horaCorta(c));
            cuando.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
            Label con = new Label(c.JE_JE_SR_JA_ME_descripcion() + " con " + c.getOdontologo().getNombre());
            con.setStyle("-fx-font-size: 14px;");
            Label estado = new Label(c.getEstado() == com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita.SOLICITADA
                    ? "Solicitud enviada: el consultorio te la confirmará." : "Cita confirmada. Llega 10 minutos antes.");
            estado.setStyle("-fx-opacity: 0.85;");
            Button cambiar = new Button("Reprogramar", JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.REPEAT, 14));
            cambiar.getStyleClass().add("secundario");
            cambiar.setOnAction(e -> dialogo.JE_JE_SR_JA_ME_reprogramar(c, x -> JE_JE_SR_JA_ME_dibujar()));
            Button cancelar = new Button("Cancelar", JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.X, 14));
            cancelar.getStyleClass().add("secundario");
            cancelar.setOnAction(e -> detalle.JE_JE_SR_JA_ME_abrir(c, this::JE_JE_SR_JA_ME_dibujar));
            HBox fila = new HBox(12, new VBox(4, new Label("TU PRÓXIMA CITA"), cuando, con, estado), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_espaciador(),
                    new VBox(8, cambiar, cancelar));
            fila.setAlignment(Pos.CENTER_LEFT);
            VBox tarjeta = new VBox(fila);
            tarjeta.getStyleClass().add("proxima-cita");
            contenido.getChildren().add(tarjeta);
        } else {
            long pendientes = planes.JE_JE_SR_JA_ME_totales(p).itemsPendientes();
            contenido.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tarjeta(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_vacio(Feather.CALENDAR, "No tienes citas programadas",
                    pendientes > 0 ? "Tienes " + pendientes + " tratamientos pendientes en tu plan. Pide una cita para continuar."
                            : "Pide una cita para tu control.")));
        }

        List<JE_JE_SR_JA_ME_Cita> todas = citas.JE_JE_SR_JA_ME_dePaciente(p);
        LocalDateTime ahora = LocalDateTime.now();
        VBox futuras = new VBox(8);
        VBox pasadas = new VBox(8);
        for (JE_JE_SR_JA_ME_Cita c : todas) {
            (c.getInicio().isAfter(ahora) ? futuras : pasadas).getChildren().add(JE_JE_SR_JA_ME_fila(c));
        }
        if (!futuras.getChildren().isEmpty()) {
            contenido.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tarjeta(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_cabecera("Próximas", null), futuras));
        }
        if (!pasadas.getChildren().isEmpty()) {
            contenido.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tarjeta(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_cabecera("Historial de citas", null), pasadas));
        }
    }

    private Node JE_JE_SR_JA_ME_fila(JE_JE_SR_JA_ME_Cita c) {
        Label dia = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(String.valueOf(c.getInicio().getDayOfMonth()), "dia");
        Label mes = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(c.getInicio().getMonth().getDisplayName(java.time.format.TextStyle.SHORT, PE).toUpperCase()
                .replace(".", "") + " " + c.getInicio().getYear() % 100, "mes");
        VBox fecha = new VBox(0, dia, mes);
        fecha.getStyleClass().add("cita-fecha");
        Label t = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(c.JE_JE_SR_JA_ME_descripcion(), "texto-fuerte");
        Label d = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(JE_JE_SR_JA_ME_AgendaVista.JE_JE_SR_JA_ME_horaCorta(c) + " · " + c.getOdontologo().getNombre(), "texto-tenue");
        VBox textos = new VBox(2, t, d);
        HBox.setHgrow(textos, Priority.ALWAYS);
        HBox h = new HBox(14, fecha, textos, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip(c.getEstado().JE_JE_SR_JA_ME_getEtiqueta(), JE_JE_SR_JA_ME_DetalleCita.JE_JE_SR_JA_ME_claseEstado(c.getEstado()), null));
        h.getStyleClass().add("cita-tarjeta");
        h.setOnMouseClicked(e -> detalle.JE_JE_SR_JA_ME_abrir(c, this::JE_JE_SR_JA_ME_dibujar));
        return h;
    }

    private static String JE_JE_SR_JA_ME_capitalizar(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
