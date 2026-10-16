package com.dentavista.vista;

import com.dentavista.modelo.Paciente;
import com.dentavista.servicio.PacienteService;
import com.dentavista.ui.Formato;
import com.dentavista.ui.Notificador;
import com.dentavista.ui.Ui;
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

/** Lista de pacientes con búsqueda instantánea por nombre o DNI. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class PacientesVista {

    private final PacienteService servicio;
    private final Notificador notificador;
    private final Navegador navegador;

    private final TableView<Paciente> tabla = new TableView<>();
    private final Map<Long, LocalDateTime> ultimas = new HashMap<>();
    private final Label contador = new Label();

    public Node vista() {
        Label titulo = Ui.etiqueta("Pacientes", "titulo-pagina");
        Label sub = Ui.etiqueta("Busca un paciente y abre su odontograma 3D.", "subtitulo-pagina");
        Button nuevo = Ui.primario("Nuevo paciente", Feather.USER_PLUS);
        nuevo.setOnAction(e -> FormularioPaciente.abrir(notificador, servicio, null, navegador::abrirFicha));
        HBox cab = new HBox(12, new VBox(2, titulo, sub), Ui.espaciador(), nuevo);
        cab.setAlignment(Pos.CENTER_LEFT);

        TextField buscar = new TextField();
        buscar.setPromptText("Buscar por nombre, apellido o DNI…");
        buscar.getStyleClass().add("buscador");
        HBox.setHgrow(buscar, Priority.ALWAYS);
        buscar.textProperty().addListener((o, a, texto) -> cargar(texto));
        contador.getStyleClass().addAll("texto-tenue");
        HBox barra = new HBox(12, Ui.icono(Feather.SEARCH, 16), buscar, contador);
        barra.setAlignment(Pos.CENTER_LEFT);

        configurarTabla();
        VBox.setVgrow(tabla, Priority.ALWAYS);
        VBox tarjeta = Ui.tarjeta(barra, tabla);
        VBox.setVgrow(tarjeta, Priority.ALWAYS);

        VBox pagina = new VBox(18, cab, tarjeta);
        pagina.getStyleClass().add("pagina");
        cargar("");
        javafx.application.Platform.runLater(buscar::requestFocus);
        return pagina;
    }

    private void cargar(String texto) {
        List<Paciente> lista = servicio.buscar(texto);
        lista.forEach(p -> ultimas.computeIfAbsent(p.getId(), id -> servicio.ultimaVisita(p).orElse(null)));
        tabla.getItems().setAll(lista);
        contador.setText(lista.size() + (lista.size() == 1 ? " paciente" : " pacientes"));
    }

    private void configurarTabla() {
        tabla.getStyleClass().addAll("tabla", "striped");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(Ui.vacio(Feather.USERS, "Sin resultados", "Prueba con otro nombre o registra un paciente nuevo."));

        TableColumn<Paciente, Paciente> nombre = columna("Paciente", p -> p);
        nombre.setPrefWidth(300);
        nombre.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Paciente p, boolean vacio) {
                super.updateItem(p, vacio);
                if (vacio || p == null) {
                    setGraphic(null);
                    return;
                }
                Label n = Ui.etiqueta(p.nombreCompleto(), "texto-fuerte");
                Label d = Ui.etiqueta("DNI " + p.getDni(), "texto-tenue", "texto-pequeno");
                HBox h = new HBox(10, Ui.avatar(p.iniciales(), p.getDni(), 34), new VBox(1, n, d));
                h.setAlignment(Pos.CENTER_LEFT);
                setGraphic(h);
            }
        });

        TableColumn<Paciente, String> edad = columna("Edad", p -> p.edad() == null ? "—" : p.edad() + " años");
        edad.setPrefWidth(90);
        TableColumn<Paciente, String> tel = columna("Teléfono", p -> p.getTelefono() == null ? "—" : p.getTelefono());
        tel.setPrefWidth(130);

        TableColumn<Paciente, Paciente> alertas = columna("Alertas médicas", p -> p);
        alertas.setPrefWidth(260);
        alertas.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Paciente p, boolean vacio) {
                super.updateItem(p, vacio);
                if (vacio || p == null) {
                    setGraphic(null);
                    return;
                }
                HBox h = new HBox(6);
                h.setAlignment(Pos.CENTER_LEFT);
                if (p.getAlergias() != null) {
                    h.getChildren().add(Ui.chip("Alergia: " + p.getAlergias(), "peligro", Feather.ALERT_OCTAGON));
                }
                if (p.getAntecedentes() != null) {
                    String a = p.getAntecedentes();
                    h.getChildren().add(Ui.chip(a.length() > 26 ? a.substring(0, 25) + "…" : a, "alerta", Feather.HEART));
                }
                if (h.getChildren().isEmpty()) {
                    h.getChildren().add(Ui.etiqueta("—", "texto-tenue"));
                }
                setGraphic(h);
            }
        });

        TableColumn<Paciente, String> visita = columna("Última visita", p -> Formato.relativo(ultimas.get(p.getId())));
        visita.setPrefWidth(130);

        TableColumn<Paciente, Paciente> acciones = columna("", p -> p);
        acciones.setPrefWidth(150);
        acciones.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Paciente p, boolean vacio) {
                super.updateItem(p, vacio);
                if (vacio || p == null) {
                    setGraphic(null);
                    return;
                }
                Button abrir = Ui.secundario("Abrir ficha", Feather.ARROW_RIGHT);
                abrir.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
                abrir.setOnAction(e -> navegador.abrirFicha(p));
                setGraphic(abrir);
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        tabla.getColumns().setAll(List.of(nombre, edad, tel, alertas, visita, acciones));
        tabla.setRowFactory(t -> {
            TableRow<Paciente> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    navegador.abrirFicha(fila.getItem());
                }
            });
            return fila;
        });
    }

    private static <T> TableColumn<Paciente, T> columna(String titulo, Function<Paciente, T> valor) {
        TableColumn<Paciente, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new SimpleObjectProperty<>(valor.apply(d.getValue())));
        c.setSortable(false);
        return c;
    }
}
