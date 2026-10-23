package com.dentavista.vista;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Rol;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Usuario;
import com.dentavista.servicio.JE_JE_SR_JA_ME_UsuarioService;
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
import javafx.scene.control.PasswordField;
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

import java.util.List;
import java.util.function.Function;

/** Usuarios del consultorio (solo administrador). */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class JE_JE_SR_JA_ME_UsuariosVista {

    private final JE_JE_SR_JA_ME_UsuarioService servicio;
    private final JE_JE_SR_JA_ME_Notificador notificador;

    private final TableView<JE_JE_SR_JA_ME_Usuario> tabla = new TableView<>();

    public Node JE_JE_SR_JA_ME_vista() {
        Label titulo = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Usuarios", "titulo-pagina");
        Label sub = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Odontólogos registran la historia clínica; la recepción solo la consulta.", "subtitulo-pagina");
        Button nuevo = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_primario("Nuevo usuario", Feather.USER_PLUS);
        nuevo.setOnAction(e -> JE_JE_SR_JA_ME_editar(null));
        HBox cab = new HBox(12, new VBox(2, titulo, sub), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_espaciador(), nuevo);
        cab.setAlignment(Pos.CENTER_LEFT);

        tabla.getStyleClass().add("tabla");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<JE_JE_SR_JA_ME_Usuario, JE_JE_SR_JA_ME_Usuario> nombre = JE_JE_SR_JA_ME_col("Nombre", u -> u);
        nombre.setPrefWidth(300);
        nombre.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(JE_JE_SR_JA_ME_Usuario u, boolean vacio) {
                super.updateItem(u, vacio);
                if (vacio || u == null) {
                    setGraphic(null);
                    return;
                }
                HBox h = new HBox(10, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_avatar(u.JE_JE_SR_JA_ME_iniciales(), u.getNombre(), 32),
                        new VBox(1, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(u.getNombre(), "texto-fuerte"),
                                JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("@" + u.getUsuario(), "texto-tenue", "texto-pequeno")));
                h.setAlignment(Pos.CENTER_LEFT);
                setGraphic(h);
            }
        });
        TableColumn<JE_JE_SR_JA_ME_Usuario, String> rol = JE_JE_SR_JA_ME_col("Rol", u -> u.getRol().JE_JE_SR_JA_ME_getEtiqueta());
        TableColumn<JE_JE_SR_JA_ME_Usuario, String> cop = JE_JE_SR_JA_ME_col("Colegiatura", u -> u.getColegiatura() == null ? "—" : u.getColegiatura());
        TableColumn<JE_JE_SR_JA_ME_Usuario, String> acceso = JE_JE_SR_JA_ME_col("Último acceso", u -> JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_relativo(u.getUltimoAcceso()));
        TableColumn<JE_JE_SR_JA_ME_Usuario, JE_JE_SR_JA_ME_Usuario> acciones = JE_JE_SR_JA_ME_col("", u -> u);
        acciones.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(JE_JE_SR_JA_ME_Usuario u, boolean vacio) {
                super.updateItem(u, vacio);
                if (vacio || u == null) {
                    setGraphic(null);
                    return;
                }
                Button ed = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.EDIT_2, "Editar");
                ed.setOnAction(e -> JE_JE_SR_JA_ME_editar(u));
                HBox h = new HBox(8, u.isActivo() ? JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Activo", "exito", null) : JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Inactivo", "neutro", null), ed);
                h.setAlignment(Pos.CENTER_RIGHT);
                setGraphic(h);
            }
        });
        tabla.getColumns().setAll(List.of(nombre, rol, cop, acceso, acciones));
        VBox.setVgrow(tabla, Priority.ALWAYS);
        VBox tarjeta = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tarjeta(tabla);
        VBox.setVgrow(tarjeta, Priority.ALWAYS);
        VBox pagina = new VBox(18, cab, tarjeta);
        pagina.getStyleClass().add("pagina");
        JE_JE_SR_JA_ME_cargar();
        return pagina;
    }

    private void JE_JE_SR_JA_ME_cargar() {
        tabla.getItems().setAll(servicio.JE_JE_SR_JA_ME_listar());
    }

    private void JE_JE_SR_JA_ME_editar(JE_JE_SR_JA_ME_Usuario existente) {
        JE_JE_SR_JA_ME_Usuario u = existente != null ? existente : JE_JE_SR_JA_ME_Usuario.builder().rol(JE_JE_SR_JA_ME_Rol.ODONTOLOGO).activo(true).build();
        TextField login = new TextField(u.getUsuario());
        TextField nombre = new TextField(u.getNombre());
        TextField email = new TextField(u.getEmail());
        com.dentavista.ui.JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_correo(email);
        com.dentavista.ui.JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_nombrePropio(nombre);
        TextField cop = new TextField(u.getColegiatura());
        cop.setPromptText("COP 12345");
        PasswordField clave = new PasswordField();
        clave.setPromptText(existente == null ? "Mínimo 6 caracteres" : "Vacío = no cambiar");
        ComboBox<JE_JE_SR_JA_ME_Rol> rol = new ComboBox<>();
        // Los accesos de pacientes se crean desde su ficha (llave en la cabecera).
        rol.getItems().setAll(JE_JE_SR_JA_ME_Rol.ADMIN, JE_JE_SR_JA_ME_Rol.ODONTOLOGO, JE_JE_SR_JA_ME_Rol.ASISTENTE);
        rol.setValue(u.getRol());
        rol.setMaxWidth(Double.MAX_VALUE);
        rol.setConverter(new StringConverter<>() {
            @Override
            public String toString(JE_JE_SR_JA_ME_Rol r) {
                return r == null ? "" : r.JE_JE_SR_JA_ME_getEtiqueta();
            }

            @Override
            public JE_JE_SR_JA_ME_Rol fromString(String s) {
                return null;
            }
        });
        CheckBox activo = new CheckBox("Cuenta activa");
        activo.setSelected(u.isActivo());

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(12);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Usuario", login), 0, 0);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Contraseña", clave), 1, 0);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Nombre completo", nombre), 0, 1, 2, 1);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Rol", rol), 0, 2);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Colegiatura", cop), 1, 2);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Correo", email), 0, 3, 2, 1);
        g.add(activo, 0, 4, 2, 1);

        notificador.JE_JE_SR_JA_ME_dialogo(Feather.SHIELD, existente == null ? "Nuevo usuario" : "Editar usuario", g, "Guardar", false, () -> {
            u.setUsuario(login.getText());
            u.setNombre(nombre.getText());
            u.setEmail(email.getText() == null || email.getText().isBlank() ? null : email.getText().trim());
            u.setColegiatura(cop.getText() == null || cop.getText().isBlank() ? null : cop.getText().trim());
            u.setRol(rol.getValue());
            u.setActivo(activo.isSelected());
            servicio.JE_JE_SR_JA_ME_guardar(u, clave.getText());
            notificador.JE_JE_SR_JA_ME_exito("Usuario " + u.getUsuario() + " guardado.");
            JE_JE_SR_JA_ME_cargar();
        }, 560);
    }

    private static <T> TableColumn<JE_JE_SR_JA_ME_Usuario, T> JE_JE_SR_JA_ME_col(String titulo, Function<JE_JE_SR_JA_ME_Usuario, T> valor) {
        TableColumn<JE_JE_SR_JA_ME_Usuario, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new SimpleObjectProperty<>(valor.apply(d.getValue())));
        c.setSortable(false);
        return c;
    }
}
