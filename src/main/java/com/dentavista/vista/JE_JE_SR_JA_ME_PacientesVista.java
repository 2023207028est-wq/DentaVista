package com.dentavista.vista;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.servicio.JE_JE_SR_JA_ME_PacienteService;
import com.dentavista.ui.JE_JE_SR_JA_ME_Formato;
import com.dentavista.ui.JE_JE_SR_JA_ME_Notificador;
import com.dentavista.ui.JE_JE_SR_JA_ME_Ui;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Lista de pacientes con búsqueda instantánea por nombre o DNI y tres filtros:
 * activos, para dar de alta (terminaron y pagaron todo) y archivados.
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class JE_JE_SR_JA_ME_PacientesVista {

    private final JE_JE_SR_JA_ME_PacienteService servicio;
    private final JE_JE_SR_JA_ME_Notificador notificador;
    private final JE_JE_SR_JA_ME_Navegador navegador;

    /** Qué grupo de pacientes se lista. */
    enum JE_JE_SR_JA_ME_Filtro { ACTIVOS, ALTA, ARCHIVADOS }

    private final TableView<JE_JE_SR_JA_ME_Paciente> tabla = new TableView<>();
    private JE_JE_SR_JA_ME_Filtro filtro = JE_JE_SR_JA_ME_Filtro.ACTIVOS;
    private String texto = "";
    private final javafx.scene.control.ToggleButton botonAlta = new javafx.scene.control.ToggleButton();
    private final Map<Long, LocalDateTime> ultimas = new HashMap<>();
    private final Label contador = new Label();

    public Node JE_JE_SR_JA_ME_vista() {
        Label titulo = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Pacientes", "titulo-pagina");
        Label sub = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Busca un paciente y abre su odontograma 3D.", "subtitulo-pagina");
        Button nuevo = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_primario("Nuevo paciente", Feather.USER_PLUS);
        nuevo.setOnAction(e -> JE_JE_SR_JA_ME_FormularioPaciente.JE_JE_SR_JA_ME_abrir(notificador, servicio, null, navegador::JE_JE_SR_JA_ME_abrirFicha));
        HBox cab = new HBox(12, new VBox(2, titulo, sub), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_espaciador(), nuevo);
        cab.setAlignment(Pos.CENTER_LEFT);

        TextField buscar = new TextField();
        buscar.setPromptText("Buscar por nombre, apellido o DNI…");
        buscar.getStyleClass().add("buscador");
        HBox.setHgrow(buscar, Priority.ALWAYS);
        buscar.textProperty().addListener((o, a, t) -> {
            texto = t;
            JE_JE_SR_JA_ME_cargar();
        });
        contador.getStyleClass().addAll("texto-tenue");
        HBox barra = new HBox(12, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.SEARCH, 16), buscar, contador, JE_JE_SR_JA_ME_filtros());
        barra.setAlignment(Pos.CENTER_LEFT);

        JE_JE_SR_JA_ME_configurarTabla();
        VBox.setVgrow(tabla, Priority.ALWAYS);
        VBox tarjeta = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tarjeta(barra, tabla);
        VBox.setVgrow(tarjeta, Priority.ALWAYS);

        VBox pagina = new VBox(18, cab, tarjeta);
        pagina.getStyleClass().add("pagina");
        JE_JE_SR_JA_ME_cargar();
        javafx.application.Platform.runLater(buscar::requestFocus);
        return pagina;
    }

    private Node JE_JE_SR_JA_ME_filtros() {
        javafx.scene.control.ToggleGroup g = new javafx.scene.control.ToggleGroup();
        javafx.scene.control.ToggleButton activos = new javafx.scene.control.ToggleButton("Activos");
        botonAlta.setGraphic(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.CHECK_CIRCLE, 14));
        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(botonAlta, "Terminaron su plan, lo pagaron y no tienen citas: puedes archivarlos");
        javafx.scene.control.ToggleButton archivados = new javafx.scene.control.ToggleButton("Archivados", JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.ARCHIVE, 14));
        activos.setUserData(JE_JE_SR_JA_ME_Filtro.ACTIVOS);
        botonAlta.setUserData(JE_JE_SR_JA_ME_Filtro.ALTA);
        archivados.setUserData(JE_JE_SR_JA_ME_Filtro.ARCHIVADOS);
        activos.getStyleClass().add("left-pill");
        botonAlta.getStyleClass().add("center-pill");
        archivados.getStyleClass().add("right-pill");
        for (var t : List.of(activos, botonAlta, archivados)) {
            t.setToggleGroup(g);
            t.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        }
        activos.setSelected(true);
        g.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                a.setSelected(true);
                return;
            }
            filtro = (JE_JE_SR_JA_ME_Filtro) b.getUserData();
            JE_JE_SR_JA_ME_cargar();
        });
        return new HBox(activos, botonAlta, archivados);
    }

    private void JE_JE_SR_JA_ME_cargar() {
        List<JE_JE_SR_JA_ME_Paciente> alta = servicio.JE_JE_SR_JA_ME_paraDarDeAlta();
        botonAlta.setText("Para dar de alta (" + alta.size() + ")");
        String q = texto == null ? "" : texto.trim().toLowerCase();
        List<JE_JE_SR_JA_ME_Paciente> lista = switch (filtro) {
            case ACTIVOS -> servicio.JE_JE_SR_JA_ME_buscar(texto);
            case ARCHIVADOS -> servicio.JE_JE_SR_JA_ME_archivados(texto);
            case ALTA -> alta.stream().filter(p -> q.isEmpty() || p.JE_JE_SR_JA_ME_nombreCompleto().toLowerCase().contains(q)
                    || p.getDni().contains(q)).toList();
        };
        lista.forEach(p -> ultimas.computeIfAbsent(p.getId(), id -> servicio.JE_JE_SR_JA_ME_ultimaVisita(p).orElse(null)));
        tabla.getItems().setAll(lista);
        contador.setText(lista.size() + (lista.size() == 1 ? " paciente" : " pacientes"));
        tabla.setPlaceholder(switch (filtro) {
            case ACTIVOS -> JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_vacio(Feather.USERS, "Sin resultados", "Prueba con otro nombre o registra un paciente nuevo.");
            case ALTA -> JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_vacio(Feather.CHECK_CIRCLE, "Nadie para dar de alta",
                    "Aquí aparecen quienes terminaron y pagaron todo su plan y no tienen citas por venir.");
            case ARCHIVADOS -> JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_vacio(Feather.ARCHIVE, "No hay pacientes archivados",
                    "Al archivar a un paciente su historia clínica se conserva y puedes reactivarlo cuando vuelva.");
        });
    }

    private void JE_JE_SR_JA_ME_archivar(JE_JE_SR_JA_ME_Paciente p) {
        Label info = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(p.JE_JE_SR_JA_ME_nombreCompleto() + " dejará de aparecer en la lista, la agenda y los avisos. "
                + "Su historia clínica, odontograma y pagos se conservan, y puedes reactivarlo cuando vuelva. "
                + "Sus citas por venir se cancelan y su acceso queda desactivado.", "texto-tenue");
        info.setWrapText(true);
        notificador.JE_JE_SR_JA_ME_dialogo(Feather.ARCHIVE, "Archivar paciente", new VBox(info), "Archivar", true, () -> {
            servicio.JE_JE_SR_JA_ME_archivar(p);
            notificador.JE_JE_SR_JA_ME_exito(p.JE_JE_SR_JA_ME_nombreCompleto() + " fue archivado.");
            JE_JE_SR_JA_ME_cargar();
        }, 480);
    }

    private void JE_JE_SR_JA_ME_reactivar(JE_JE_SR_JA_ME_Paciente p) {
        if (notificador.JE_JE_SR_JA_ME_intentar(() -> servicio.JE_JE_SR_JA_ME_reactivar(p))) {
            notificador.JE_JE_SR_JA_ME_exito(p.JE_JE_SR_JA_ME_nombreCompleto() + " vuelve a estar activo.");
            JE_JE_SR_JA_ME_cargar();
        }
    }

    private void JE_JE_SR_JA_ME_configurarTabla() {
        tabla.getStyleClass().addAll("tabla", "striped");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<JE_JE_SR_JA_ME_Paciente, JE_JE_SR_JA_ME_Paciente> nombre = JE_JE_SR_JA_ME_columna("Paciente", p -> p);
        nombre.setPrefWidth(300);
        nombre.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(JE_JE_SR_JA_ME_Paciente p, boolean vacio) {
                super.updateItem(p, vacio);
                if (vacio || p == null) {
                    setGraphic(null);
                    return;
                }
                Label n = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(p.JE_JE_SR_JA_ME_nombreCompleto(), "texto-fuerte");
                Label d = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("DNI " + p.getDni(), "texto-tenue", "texto-pequeno");
                HBox h = new HBox(10, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_avatar(p.JE_JE_SR_JA_ME_iniciales(), p.getDni(), 34), new VBox(1, n, d));
                h.setAlignment(Pos.CENTER_LEFT);
                setGraphic(h);
            }
        });

        TableColumn<JE_JE_SR_JA_ME_Paciente, String> edad = JE_JE_SR_JA_ME_columna("Edad", p -> p.JE_JE_SR_JA_ME_edad() == null ? "—" : p.JE_JE_SR_JA_ME_edad() + " años");
        edad.setPrefWidth(90);
        TableColumn<JE_JE_SR_JA_ME_Paciente, String> tel = JE_JE_SR_JA_ME_columna("Teléfono", p -> p.getTelefono() == null ? "—" : p.getTelefono());
        tel.setPrefWidth(130);

        TableColumn<JE_JE_SR_JA_ME_Paciente, JE_JE_SR_JA_ME_Paciente> alertas = JE_JE_SR_JA_ME_columna("Alertas médicas", p -> p);
        alertas.setPrefWidth(260);
        alertas.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(JE_JE_SR_JA_ME_Paciente p, boolean vacio) {
                super.updateItem(p, vacio);
                if (vacio || p == null) {
                    setGraphic(null);
                    return;
                }
                HBox h = new HBox(6);
                h.setAlignment(Pos.CENTER_LEFT);
                if (p.getAlergias() != null) {
                    h.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Alergia: " + p.getAlergias(), "peligro", Feather.ALERT_OCTAGON));
                }
                if (p.getAntecedentes() != null) {
                    String a = p.getAntecedentes();
                    h.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip(a.length() > 26 ? a.substring(0, 25) + "…" : a, "alerta", Feather.HEART));
                }
                if (h.getChildren().isEmpty()) {
                    h.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("—", "texto-tenue"));
                }
                setGraphic(h);
            }
        });

        TableColumn<JE_JE_SR_JA_ME_Paciente, String> visita = JE_JE_SR_JA_ME_columna("Última visita", p -> JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_relativo(ultimas.get(p.getId())));
        visita.setPrefWidth(130);

        TableColumn<JE_JE_SR_JA_ME_Paciente, JE_JE_SR_JA_ME_Paciente> acciones = JE_JE_SR_JA_ME_columna("", p -> p);
        acciones.setPrefWidth(200);
        acciones.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(JE_JE_SR_JA_ME_Paciente p, boolean vacio) {
                super.updateItem(p, vacio);
                if (vacio || p == null) {
                    setGraphic(null);
                    return;
                }
                Button abrir = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("Abrir ficha", Feather.ARROW_RIGHT);
                abrir.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
                abrir.setOnAction(e -> navegador.JE_JE_SR_JA_ME_abrirFicha(p));
                Button extra;
                if (p.isActivo()) {
                    extra = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.ARCHIVE, "Archivar (dar de baja)");
                    extra.setOnAction(e -> JE_JE_SR_JA_ME_archivar(p));
                } else {
                    extra = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.ROTATE_CCW, "Reactivar paciente");
                    extra.setOnAction(e -> JE_JE_SR_JA_ME_reactivar(p));
                }
                HBox h = new HBox(6, extra, abrir);
                h.setAlignment(Pos.CENTER_RIGHT);
                setGraphic(h);
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        tabla.getColumns().setAll(List.of(nombre, edad, tel, alertas, visita, acciones));
        tabla.setRowFactory(t -> {
            TableRow<JE_JE_SR_JA_ME_Paciente> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    navegador.JE_JE_SR_JA_ME_abrirFicha(fila.getItem());
                }
            });
            return fila;
        });
    }

    private static <T> TableColumn<JE_JE_SR_JA_ME_Paciente, T> JE_JE_SR_JA_ME_columna(String titulo, Function<JE_JE_SR_JA_ME_Paciente, T> valor) {
        TableColumn<JE_JE_SR_JA_ME_Paciente, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new SimpleObjectProperty<>(valor.apply(d.getValue())));
        c.setSortable(false);
        return c;
    }
}
