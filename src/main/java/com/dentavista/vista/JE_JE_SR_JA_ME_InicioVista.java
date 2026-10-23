package com.dentavista.vista;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Cita;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Consulta;
import com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.servicio.JE_JE_SR_JA_ME_CitaService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_EstadisticaService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_PacienteService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_PagoService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_SesionService;
import com.dentavista.ui.JE_JE_SR_JA_ME_Formato;
import com.dentavista.ui.JE_JE_SR_JA_ME_Notificador;
import com.dentavista.ui.JE_JE_SR_JA_ME_Ui;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Inicio: lo que el consultorio necesita ver al empezar el día. Citas de hoy,
 * solicitudes por confirmar, pacientes que deben y pacientes con tratamientos
 * pendientes que todavía no tienen cita.
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class JE_JE_SR_JA_ME_InicioVista {

    private final JE_JE_SR_JA_ME_EstadisticaService estadisticas;
    private final JE_JE_SR_JA_ME_SesionService sesion;
    private final JE_JE_SR_JA_ME_PacienteService pacientes;
    private final JE_JE_SR_JA_ME_Notificador notificador;
    private final JE_JE_SR_JA_ME_Navegador navegador;
    private final JE_JE_SR_JA_ME_CitaService citas;
    private final JE_JE_SR_JA_ME_DetalleCita detalle;
    private final JE_JE_SR_JA_ME_DialogoCita dialogoCita;
    private final JE_JE_SR_JA_ME_PagoService pagos;

    public Node JE_JE_SR_JA_ME_vista() {
        JE_JE_SR_JA_ME_EstadisticaService.JE_JE_SR_JA_ME_Resumen r = estadisticas.JE_JE_SR_JA_ME_resumen();

        String nombre = sesion.JE_JE_SR_JA_ME_getUsuario().getNombre();
        Label saludo = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(JE_JE_SR_JA_ME_saludo() + ", " + JE_JE_SR_JA_ME_primerNombre(nombre), "titulo-pagina");
        Label fecha = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_fechaLarga(LocalDate.now().atStartOfDay()), "subtitulo-pagina");
        Button cita = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("Nueva cita", Feather.CALENDAR);
        cita.setOnAction(e -> dialogoCita.JE_JE_SR_JA_ME_abrir(new JE_JE_SR_JA_ME_DialogoCita.JE_JE_SR_JA_ME_Sugerencia(null, null, null, null),
                c -> navegador.JE_JE_SR_JA_ME_irA(JE_JE_SR_JA_ME_Navegador.JE_JE_SR_JA_ME_Seccion.INICIO)));
        Button nuevo = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_primario("Nuevo paciente", Feather.USER_PLUS);
        nuevo.setOnAction(e -> JE_JE_SR_JA_ME_FormularioPaciente.JE_JE_SR_JA_ME_abrir(notificador, pacientes, null, navegador::JE_JE_SR_JA_ME_abrirFicha));
        HBox cab = new HBox(10, new VBox(2, saludo, fecha), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_espaciador(), cita, nuevo);
        cab.setAlignment(Pos.CENTER_LEFT);

        List<JE_JE_SR_JA_ME_Cita> hoy = citas.JE_JE_SR_JA_ME_delDia(LocalDate.now()).stream().filter(c -> c.getEstado() != JE_JE_SR_JA_ME_EstadoCita.CANCELADA).toList();
        long pendientesHoy = hoy.stream().filter(c -> c.getEstado().JE_JE_SR_JA_ME_abierta()).count();

        GridPane kpis = new GridPane();
        kpis.setHgap(16);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(25);
            kpis.getColumnConstraints().add(c);
        }
        kpis.add(JE_JE_SR_JA_ME_kpi(Feather.CALENDAR, "tono-turquesa", String.valueOf(hoy.size()),
                "Citas hoy · " + pendientesHoy + " por atender"), 0, 0);
        kpis.add(JE_JE_SR_JA_ME_kpi(Feather.USERS, "tono-azul", String.valueOf(r.pacientes()),
                "Pacientes activos · " + r.consultasMes() + " consultas este mes"), 1, 0);
        kpis.add(JE_JE_SR_JA_ME_kpi(Feather.DOLLAR_SIGN, "tono-violeta", JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(pagos.JE_JE_SR_JA_ME_cobradoEsteMes()),
                "Cobrado este mes · " + pagos.JE_JE_SR_JA_ME_pagosEsteMes() + " pagos"), 2, 0);
        int alta = pacientes.JE_JE_SR_JA_ME_paraDarDeAlta().size();
        Node kpiAlta = JE_JE_SR_JA_ME_kpi(Feather.CHECK_CIRCLE, "tono-ambar", String.valueOf(alta),
                "Para dar de alta · terminaron y pagaron todo");
        kpiAlta.getStyleClass().add("kpi-enlace");
        kpiAlta.setOnMouseClicked(e -> navegador.JE_JE_SR_JA_ME_irA(JE_JE_SR_JA_ME_Navegador.JE_JE_SR_JA_ME_Seccion.PACIENTES));
        kpis.add(kpiAlta, 3, 0);

        // Columna izquierda: el día.
        VBox izquierda = new VBox(16, JE_JE_SR_JA_ME_tarjetaCitasHoy(hoy), JE_JE_SR_JA_ME_tarjetaPorAgendar());
        HBox.setHgrow(izquierda, Priority.ALWAYS);

        // Columna derecha: pendientes de gestión.
        VBox derecha = new VBox(16);
        List<JE_JE_SR_JA_ME_Cita> solicitudes = citas.JE_JE_SR_JA_ME_solicitudes();
        if (!solicitudes.isEmpty()) {
            derecha.getChildren().add(JE_JE_SR_JA_ME_tarjetaSolicitudes(solicitudes));
        }
        derecha.getChildren().addAll(JE_JE_SR_JA_ME_tarjetaDeudores(), JE_JE_SR_JA_ME_tarjetaConsultas());
        derecha.setMinWidth(380);
        derecha.setPrefWidth(420);
        derecha.setMaxWidth(460);

        VBox pagina = new VBox(20, cab, kpis, new HBox(16, izquierda, derecha));
        pagina.getStyleClass().add("pagina");
        ScrollPane scroll = new ScrollPane(pagina);
        scroll.setFitToWidth(true);
        return scroll;
    }

    // ---------------------------------------------------------------- tarjetas

    private VBox JE_JE_SR_JA_ME_tarjetaCitasHoy(List<JE_JE_SR_JA_ME_Cita> hoy) {
        VBox lista = new VBox(6);
        for (JE_JE_SR_JA_ME_Cita c : hoy) {
            Label hora = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(c.getInicio().toLocalTime().toString(), "texto-fuerte");
            hora.setMinWidth(46);
            Label n = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(c.getPaciente().JE_JE_SR_JA_ME_nombreCompleto(), "texto-fuerte");
            Label d = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(c.JE_JE_SR_JA_ME_descripcion() + " · " + c.getOdontologo().getNombre(), "texto-tenue", "texto-pequeno");
            VBox textos = new VBox(1, n, d);
            textos.setMinWidth(0);
            HBox.setHgrow(textos, Priority.ALWAYS);
            Label est = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip(c.getEstado().JE_JE_SR_JA_ME_getEtiqueta(), JE_JE_SR_JA_ME_DetalleCita.JE_JE_SR_JA_ME_claseEstado(c.getEstado()), null);
            est.setMinWidth(Region.USE_PREF_SIZE);
            HBox f = new HBox(12, hora, textos, est);
            if (c.getPaciente().getAlergias() != null) {
                Label al = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Alergia", "peligro", Feather.ALERT_OCTAGON);
                al.setMinWidth(Region.USE_PREF_SIZE);
                JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(al, "Alergia: " + c.getPaciente().getAlergias());
                f.getChildren().add(2, al);
            }
            f.setAlignment(Pos.CENTER_LEFT);
            f.getStyleClass().add("cita-tarjeta");
            f.setOnMouseClicked(e -> detalle.JE_JE_SR_JA_ME_abrir(c, () -> navegador.JE_JE_SR_JA_ME_irA(JE_JE_SR_JA_ME_Navegador.JE_JE_SR_JA_ME_Seccion.INICIO)));
            lista.getChildren().add(f);
        }
        if (hoy.isEmpty()) {
            lista.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_vacio(Feather.CALENDAR, "Sin citas para hoy", "Agenda una cita desde aquí o desde la agenda."));
        }
        Button agenda = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("Ver agenda", Feather.CALENDAR);
        agenda.setOnAction(e -> navegador.JE_JE_SR_JA_ME_irA(JE_JE_SR_JA_ME_Navegador.JE_JE_SR_JA_ME_Seccion.AGENDA));
        return JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tarjeta(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_cabecera("Citas de hoy", "Clic en una cita para atenderla, confirmarla o enviar el recordatorio", agenda),
                lista);
    }

    private VBox JE_JE_SR_JA_ME_tarjetaPorAgendar() {
        VBox lista = new VBox(6);
        List<JE_JE_SR_JA_ME_CitaService.JE_JE_SR_JA_ME_PorAgendar> porAgendar = citas.JE_JE_SR_JA_ME_pacientesPorAgendar();
        for (JE_JE_SR_JA_ME_CitaService.JE_JE_SR_JA_ME_PorAgendar a : porAgendar.stream().limit(6).toList()) {
            JE_JE_SR_JA_ME_Paciente p = a.paciente();
            Label n = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(p.JE_JE_SR_JA_ME_nombreCompleto(), "texto-fuerte");
            Label d = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(a.tratamientos() + (a.tratamientos() == 1 ? " tratamiento pendiente · " : " tratamientos pendientes · ")
                    + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(a.monto()) + (p.getTelefono() != null ? " · " + p.getTelefono() : ""), "texto-tenue", "texto-pequeno");
            VBox textos = new VBox(1, n, d);
            HBox.setHgrow(textos, Priority.ALWAYS);
            Button agendar = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("Agendar", Feather.CALENDAR);
            agendar.setMinWidth(Region.USE_PREF_SIZE);
            agendar.setOnAction(e -> dialogoCita.JE_JE_SR_JA_ME_abrir(new JE_JE_SR_JA_ME_DialogoCita.JE_JE_SR_JA_ME_Sugerencia(p, null, null, null),
                    c -> navegador.JE_JE_SR_JA_ME_irA(JE_JE_SR_JA_ME_Navegador.JE_JE_SR_JA_ME_Seccion.INICIO)));
            Button ficha = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.ARROW_RIGHT, "Abrir ficha");
            ficha.setOnAction(e -> navegador.JE_JE_SR_JA_ME_abrirFicha(p));
            HBox f = new HBox(10, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_avatar(p.JE_JE_SR_JA_ME_iniciales(), p.getDni(), 32), textos, agendar, ficha);
            f.setAlignment(Pos.CENTER_LEFT);
            f.getStyleClass().add("cita-tarjeta");
            lista.getChildren().add(f);
        }
        if (porAgendar.isEmpty()) {
            lista.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Todos los pacientes con tratamientos pendientes ya tienen cita.", "texto-tenue"));
        }
        return JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tarjeta(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_cabecera("Tratamientos sin cita",
                porAgendar.size() + " pacientes con tratamientos pendientes y sin cita próxima: conviene llamarlos"), lista);
    }

    private VBox JE_JE_SR_JA_ME_tarjetaSolicitudes(List<JE_JE_SR_JA_ME_Cita> solicitudes) {
        VBox lista = new VBox(6);
        for (JE_JE_SR_JA_ME_Cita c : solicitudes) {
            Label n = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(c.getPaciente().JE_JE_SR_JA_ME_nombreCompleto(), "texto-fuerte");
            Label d = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_fechaHora(c.getInicio()) + " · " + c.getOdontologo().getNombre(), "texto-tenue", "texto-pequeno");
            VBox textos = new VBox(1, n, d);
            HBox.setHgrow(textos, Priority.ALWAYS);
            Button ok = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.CHECK, "Confirmar cita");
            ok.setOnAction(e -> {
                if (notificador.JE_JE_SR_JA_ME_intentar(() -> citas.JE_JE_SR_JA_ME_confirmar(c))) {
                    notificador.JE_JE_SR_JA_ME_exito("Cita de " + c.getPaciente().JE_JE_SR_JA_ME_nombreCompleto() + " confirmada.");
                    navegador.JE_JE_SR_JA_ME_irA(JE_JE_SR_JA_ME_Navegador.JE_JE_SR_JA_ME_Seccion.INICIO);
                }
            });
            Button ver = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.EYE, "Ver detalle");
            ver.setOnAction(e -> detalle.JE_JE_SR_JA_ME_abrir(c, () -> navegador.JE_JE_SR_JA_ME_irA(JE_JE_SR_JA_ME_Navegador.JE_JE_SR_JA_ME_Seccion.INICIO)));
            HBox f = new HBox(8, textos, ver, ok);
            f.setAlignment(Pos.CENTER_LEFT);
            f.getStyleClass().add("cita-tarjeta");
            lista.getChildren().add(f);
        }
        return JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tarjeta(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_cabecera("Solicitudes por confirmar", "Pedidas por los pacientes desde su acceso"), lista);
    }

    private VBox JE_JE_SR_JA_ME_tarjetaDeudores() {
        VBox lista = new VBox(4);
        List<JE_JE_SR_JA_ME_PagoService.JE_JE_SR_JA_ME_Deudor> deudores = pagos.JE_JE_SR_JA_ME_deudores();
        for (JE_JE_SR_JA_ME_PagoService.JE_JE_SR_JA_ME_Deudor d : deudores.stream().limit(5).toList()) {
            Label n = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(d.paciente().JE_JE_SR_JA_ME_nombreCompleto(), "texto-fuerte");
            Label m = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(d.deuda()), "texto-fuerte");
            m.setStyle("-fx-text-fill: #dc2626;");
            m.setMinWidth(Region.USE_PREF_SIZE);
            HBox f = new HBox(10, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_avatar(d.paciente().JE_JE_SR_JA_ME_iniciales(), d.paciente().getDni(), 30), n, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_espaciador(), m);
            f.setAlignment(Pos.CENTER_LEFT);
            f.getStyleClass().add("cita-tarjeta");
            f.setOnMouseClicked(e -> navegador.JE_JE_SR_JA_ME_abrirFicha(d.paciente()));
            lista.getChildren().add(f);
        }
        if (deudores.isEmpty()) {
            lista.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Nadie tiene saldo pendiente.", "texto-tenue"));
        }
        java.math.BigDecimal total = deudores.stream().map(JE_JE_SR_JA_ME_PagoService.JE_JE_SR_JA_ME_Deudor::deuda)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        return JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tarjeta(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_cabecera("Por cobrar · " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(total),
                deudores.size() + " pacientes deben tratamientos ya realizados"), lista);
    }

    private VBox JE_JE_SR_JA_ME_tarjetaConsultas() {
        VBox lista = new VBox(4);
        for (JE_JE_SR_JA_ME_Consulta c : estadisticas.JE_JE_SR_JA_ME_ultimasConsultas().stream().limit(5).toList()) {
            JE_JE_SR_JA_ME_Paciente p = c.getPaciente();
            Label n = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(p.JE_JE_SR_JA_ME_nombreCompleto(), "texto-fuerte");
            Label m = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(c.getMotivo(), "texto-tenue", "texto-pequeno");
            m.setMaxWidth(220);
            Label f = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_relativo(c.getFecha()), "texto-tenue", "texto-pequeno");
            f.setMinWidth(Region.USE_PREF_SIZE);
            HBox h = new HBox(10, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_avatar(p.JE_JE_SR_JA_ME_iniciales(), p.getDni(), 30), new VBox(1, n, m), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_espaciador(), f);
            h.setAlignment(Pos.CENTER_LEFT);
            h.getStyleClass().add("cita-tarjeta");
            h.setOnMouseClicked(e -> navegador.JE_JE_SR_JA_ME_abrirFicha(p));
            lista.getChildren().add(h);
        }
        return JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tarjeta(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_cabecera("Últimas consultas", null), lista);
    }

    private Node JE_JE_SR_JA_ME_kpi(Ikon icono, String tono, String valor, String rotulo) {
        StackPane ic = new StackPane(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(icono, 18));
        ic.getStyleClass().addAll("kpi-icono", tono);
        Label v = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(valor, "kpi-valor");
        Label r = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(rotulo, "kpi-rotulo");
        r.setWrapText(true);
        VBox caja = new VBox(8, ic, v, r);
        caja.getStyleClass().add("kpi");
        caja.setMaxWidth(Double.MAX_VALUE);
        return caja;
    }

    private static String JE_JE_SR_JA_ME_saludo() {
        LocalTime t = LocalTime.now();
        return t.getHour() < 12 ? "Buenos días" : t.getHour() < 19 ? "Buenas tardes" : "Buenas noches";
    }

    /** "Dra. Lucía Paredes Quispe" -> "Dra. Lucía". */
    private static String JE_JE_SR_JA_ME_primerNombre(String nombre) {
        String[] p = nombre.split(" ");
        if (p.length >= 2 && p[0].endsWith(".")) {
            return p[0] + " " + p[1];
        }
        return p[0];
    }
}
