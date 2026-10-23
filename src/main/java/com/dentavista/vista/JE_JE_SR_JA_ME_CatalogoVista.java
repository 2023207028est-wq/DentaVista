package com.dentavista.vista;

import com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Tratamiento;
import com.dentavista.servicio.JE_JE_SR_JA_ME_CatalogoService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_NegocioException;
import com.dentavista.servicio.JE_JE_SR_JA_ME_SesionService;
import com.dentavista.ui.JE_JE_SR_JA_ME_Formato;
import com.dentavista.ui.JE_JE_SR_JA_ME_Notificador;
import com.dentavista.ui.JE_JE_SR_JA_ME_Ui;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

/** Catálogo de tratamientos: precios y efecto de cada uno en el odontograma. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class JE_JE_SR_JA_ME_CatalogoVista {

    private final JE_JE_SR_JA_ME_CatalogoService servicio;
    private final JE_JE_SR_JA_ME_SesionService sesion;
    private final JE_JE_SR_JA_ME_Notificador notificador;

    private final TableView<JE_JE_SR_JA_ME_Tratamiento> tabla = new TableView<>();

    public Node JE_JE_SR_JA_ME_vista() {
        Label titulo = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Tratamientos y precios", "titulo-pagina");
        Label sub = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Al marcar un tratamiento como realizado, el odontograma se actualiza con su estado resultante.",
                "subtitulo-pagina");
        Button nuevo = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_primario("Nuevo tratamiento", Feather.PLUS);
        nuevo.setOnAction(e -> JE_JE_SR_JA_ME_editar(null));
        nuevo.setVisible(sesion.JE_JE_SR_JA_ME_puedeAdministrar());
        HBox cab = new HBox(12, new VBox(2, titulo, sub), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_espaciador(), nuevo);
        cab.setAlignment(Pos.CENTER_LEFT);

        JE_JE_SR_JA_ME_configurar();
        VBox.setVgrow(tabla, Priority.ALWAYS);
        VBox tarjeta = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tarjeta(tabla);
        VBox.setVgrow(tarjeta, Priority.ALWAYS);
        if (!sesion.JE_JE_SR_JA_ME_puedeAdministrar()) {
            tarjeta.getChildren().add(0, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Solo el administrador puede modificar precios", "info", Feather.LOCK));
        }
        VBox pagina = new VBox(18, cab, tarjeta);
        pagina.getStyleClass().add("pagina");
        JE_JE_SR_JA_ME_cargar();
        return pagina;
    }

    private void JE_JE_SR_JA_ME_cargar() {
        tabla.getItems().setAll(servicio.JE_JE_SR_JA_ME_todos());
    }

    private void JE_JE_SR_JA_ME_configurar() {
        tabla.getStyleClass().add("tabla");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<JE_JE_SR_JA_ME_Tratamiento, String> codigo = JE_JE_SR_JA_ME_col("Código", JE_JE_SR_JA_ME_Tratamiento::getCodigo);
        codigo.setPrefWidth(80);
        TableColumn<JE_JE_SR_JA_ME_Tratamiento, JE_JE_SR_JA_ME_Tratamiento> nombre = JE_JE_SR_JA_ME_col("Tratamiento", t -> t);
        nombre.setPrefWidth(320);
        nombre.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(JE_JE_SR_JA_ME_Tratamiento t, boolean vacio) {
                super.updateItem(t, vacio);
                if (vacio || t == null) {
                    setGraphic(null);
                    return;
                }
                VBox v = new VBox(1, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(t.getNombre(), "texto-fuerte"));
                if (t.getDescripcion() != null) {
                    v.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(t.getDescripcion(), "texto-tenue", "texto-pequeno"));
                }
                setGraphic(v);
            }
        });
        TableColumn<JE_JE_SR_JA_ME_Tratamiento, JE_JE_SR_JA_ME_Tratamiento> efecto = JE_JE_SR_JA_ME_col("Deja la pieza como", t -> t);
        efecto.setPrefWidth(200);
        efecto.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(JE_JE_SR_JA_ME_Tratamiento t, boolean vacio) {
                super.updateItem(t, vacio);
                if (vacio || t == null) {
                    setGraphic(null);
                    return;
                }
                JE_JE_SR_JA_ME_EstadoDental e = t.getEstadoResultante();
                Label l = e == null ? JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("No cambia el odontograma", "texto-tenue")
                        : new Label(e.JE_JE_SR_JA_ME_getEtiqueta(), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_muestra(e.JE_JE_SR_JA_ME_getColor()));
                l.setGraphicTextGap(8);
                setGraphic(l);
            }
        });
        TableColumn<JE_JE_SR_JA_ME_Tratamiento, String> alcance = JE_JE_SR_JA_ME_col("Se cobra", t -> !t.isRequierePieza() ? "Por sesión"
                : t.isPorCara() ? "Por cara" : "Por pieza");
        alcance.setPrefWidth(100);
        TableColumn<JE_JE_SR_JA_ME_Tratamiento, String> precio = JE_JE_SR_JA_ME_col("Precio", t -> JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(t.getPrecio()));
        precio.setPrefWidth(110);
        precio.setStyle("-fx-alignment: CENTER-RIGHT;");
        TableColumn<JE_JE_SR_JA_ME_Tratamiento, JE_JE_SR_JA_ME_Tratamiento> estado = JE_JE_SR_JA_ME_col("", t -> t);
        estado.setPrefWidth(150);
        estado.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(JE_JE_SR_JA_ME_Tratamiento t, boolean vacio) {
                super.updateItem(t, vacio);
                if (vacio || t == null) {
                    setGraphic(null);
                    return;
                }
                HBox h = new HBox(8, t.isActivo() ? JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Activo", "exito", null) : JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Inactivo", "neutro", null));
                h.setAlignment(Pos.CENTER_RIGHT);
                if (sesion.JE_JE_SR_JA_ME_puedeAdministrar()) {
                    Button ed = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.EDIT_2, "Editar");
                    ed.setOnAction(e -> JE_JE_SR_JA_ME_editar(t));
                    h.getChildren().add(ed);
                }
                setGraphic(h);
            }
        });
        tabla.getColumns().setAll(List.of(codigo, nombre, efecto, alcance, precio, estado));
    }

    private void JE_JE_SR_JA_ME_editar(JE_JE_SR_JA_ME_Tratamiento existente) {
        JE_JE_SR_JA_ME_Tratamiento t = existente != null ? existente : JE_JE_SR_JA_ME_Tratamiento.builder().requierePieza(true).activo(true).build();
        TextField codigo = new TextField(t.getCodigo());
        TextField nombre = new TextField(t.getNombre());
        TextField desc = new TextField(t.getDescripcion());
        TextField precio = new TextField(t.getPrecio() == null ? "" : t.getPrecio().toPlainString());
        com.dentavista.ui.JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_dinero(precio);
        ComboBox<JE_JE_SR_JA_ME_EstadoDental> resultado = new ComboBox<>();
        resultado.getItems().add(null);
        for (JE_JE_SR_JA_ME_EstadoDental e : JE_JE_SR_JA_ME_EstadoDental.values()) {
            if (!e.JE_JE_SR_JA_ME_esPatologia() && e != JE_JE_SR_JA_ME_EstadoDental.SANO) {
                resultado.getItems().add(e);
            }
        }
        resultado.setValue(t.getEstadoResultante());
        resultado.setMaxWidth(Double.MAX_VALUE);
        resultado.setConverter(new StringConverter<>() {
            @Override
            public String toString(JE_JE_SR_JA_ME_EstadoDental e) {
                return e == null ? "No cambia el odontograma" : e.JE_JE_SR_JA_ME_getEtiqueta();
            }

            @Override
            public JE_JE_SR_JA_ME_EstadoDental fromString(String s) {
                return null;
            }
        });
        CheckBox porCara = new CheckBox("Se aplica y cobra por cara");
        porCara.setSelected(t.isPorCara());
        CheckBox requiere = new CheckBox("Se realiza sobre una pieza");
        requiere.setSelected(t.isRequierePieza());
        CheckBox activo = new CheckBox("Activo (disponible para nuevos planes)");
        activo.setSelected(t.isActivo());

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(12);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Código", codigo), 0, 0);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Precio (S/)", precio), 1, 0);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Nombre", nombre), 0, 1, 2, 1);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Descripción", desc), 0, 2, 2, 1);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Estado resultante en el odontograma", resultado), 0, 3, 2, 1);
        g.add(new VBox(8, requiere, porCara, activo), 0, 4, 2, 1);

        notificador.JE_JE_SR_JA_ME_dialogo(Feather.LAYERS, existente == null ? "Nuevo tratamiento" : "Editar tratamiento", g,
                "Guardar", false, () -> {
                    t.setCodigo(codigo.getText());
                    t.setNombre(nombre.getText());
                    t.setDescripcion(desc.getText() == null || desc.getText().isBlank() ? null : desc.getText().trim());
                    try {
                        t.setPrecio(new BigDecimal(precio.getText().trim().replace(",", ".")));
                    } catch (NumberFormatException ex) {
                        throw new JE_JE_SR_JA_ME_NegocioException("El precio debe ser un número, por ejemplo 120.50");
                    }
                    t.setEstadoResultante(resultado.getValue());
                    t.setPorCara(porCara.isSelected());
                    t.setRequierePieza(requiere.isSelected() || porCara.isSelected());
                    t.setActivo(activo.isSelected());
                    servicio.JE_JE_SR_JA_ME_guardar(t);
                    notificador.JE_JE_SR_JA_ME_exito("Tratamiento \"" + t.getNombre() + "\" guardado.");
                    JE_JE_SR_JA_ME_cargar();
                }, 560);
    }

    private static <T> TableColumn<JE_JE_SR_JA_ME_Tratamiento, T> JE_JE_SR_JA_ME_col(String titulo, Function<JE_JE_SR_JA_ME_Tratamiento, T> valor) {
        TableColumn<JE_JE_SR_JA_ME_Tratamiento, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new SimpleObjectProperty<>(valor.apply(d.getValue())));
        c.setSortable(false);
        return c;
    }
}
