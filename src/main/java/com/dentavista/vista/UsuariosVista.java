package com.dentavista.vista;

import com.dentavista.modelo.Rol;
import com.dentavista.modelo.Usuario;
import com.dentavista.servicio.UsuarioService;
import com.dentavista.ui.Formato;
import com.dentavista.ui.Notificador;
import com.dentavista.ui.Ui;
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
public class UsuariosVista {

    private final UsuarioService servicio;
    private final Notificador notificador;

    private final TableView<Usuario> tabla = new TableView<>();

    public Node vista() {
        Label titulo = Ui.etiqueta("Usuarios", "titulo-pagina");
        Label sub = Ui.etiqueta("Odontólogos registran la historia clínica; la recepción solo la consulta.", "subtitulo-pagina");
        Button nuevo = Ui.primario("Nuevo usuario", Feather.USER_PLUS);
        nuevo.setOnAction(e -> editar(null));
        HBox cab = new HBox(12, new VBox(2, titulo, sub), Ui.espaciador(), nuevo);
        cab.setAlignment(Pos.CENTER_LEFT);

        tabla.getStyleClass().add("tabla");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<Usuario, Usuario> nombre = col("Nombre", u -> u);
        nombre.setPrefWidth(300);
        nombre.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Usuario u, boolean vacio) {
                super.updateItem(u, vacio);
                if (vacio || u == null) {
                    setGraphic(null);
                    return;
                }
                HBox h = new HBox(10, Ui.avatar(u.iniciales(), u.getNombre(), 32),
                        new VBox(1, Ui.etiqueta(u.getNombre(), "texto-fuerte"),
                                Ui.etiqueta("@" + u.getUsuario(), "texto-tenue", "texto-pequeno")));
                h.setAlignment(Pos.CENTER_LEFT);
                setGraphic(h);
            }
        });
        TableColumn<Usuario, String> rol = col("Rol", u -> u.getRol().getEtiqueta());
        TableColumn<Usuario, String> cop = col("Colegiatura", u -> u.getColegiatura() == null ? "—" : u.getColegiatura());
        TableColumn<Usuario, String> acceso = col("Último acceso", u -> Formato.relativo(u.getUltimoAcceso()));
        TableColumn<Usuario, Usuario> acciones = col("", u -> u);
        acciones.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Usuario u, boolean vacio) {
                super.updateItem(u, vacio);
                if (vacio || u == null) {
                    setGraphic(null);
                    return;
                }
                Button ed = Ui.botonIcono(Feather.EDIT_2, "Editar");
                ed.setOnAction(e -> editar(u));
                HBox h = new HBox(8, u.isActivo() ? Ui.chip("Activo", "exito", null) : Ui.chip("Inactivo", "neutro", null), ed);
                h.setAlignment(Pos.CENTER_RIGHT);
                setGraphic(h);
            }
        });
        tabla.getColumns().setAll(List.of(nombre, rol, cop, acceso, acciones));
        VBox.setVgrow(tabla, Priority.ALWAYS);
        VBox tarjeta = Ui.tarjeta(tabla);
        VBox.setVgrow(tarjeta, Priority.ALWAYS);
        VBox pagina = new VBox(18, cab, tarjeta);
        pagina.getStyleClass().add("pagina");
        cargar();
        return pagina;
    }

    private void cargar() {
        tabla.getItems().setAll(servicio.listar());
    }

    private void editar(Usuario existente) {
        Usuario u = existente != null ? existente : Usuario.builder().rol(Rol.ODONTOLOGO).activo(true).build();
        TextField login = new TextField(u.getUsuario());
        TextField nombre = new TextField(u.getNombre());
        TextField email = new TextField(u.getEmail());
        com.dentavista.ui.Mascaras.correo(email);
        com.dentavista.ui.Mascaras.nombrePropio(nombre);
        TextField cop = new TextField(u.getColegiatura());
        cop.setPromptText("COP 12345");
        PasswordField clave = new PasswordField();
        clave.setPromptText(existente == null ? "Mínimo 6 caracteres" : "Vacío = no cambiar");
        ComboBox<Rol> rol = new ComboBox<>();
        // Los accesos de pacientes se crean desde su ficha (llave en la cabecera).
        rol.getItems().setAll(Rol.ADMIN, Rol.ODONTOLOGO, Rol.ASISTENTE);
        rol.setValue(u.getRol());
        rol.setMaxWidth(Double.MAX_VALUE);
        rol.setConverter(new StringConverter<>() {
            @Override
            public String toString(Rol r) {
                return r == null ? "" : r.getEtiqueta();
            }

            @Override
            public Rol fromString(String s) {
                return null;
            }
        });
        CheckBox activo = new CheckBox("Cuenta activa");
        activo.setSelected(u.isActivo());

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(12);
        g.add(Ui.campo("Usuario", login), 0, 0);
        g.add(Ui.campo("Contraseña", clave), 1, 0);
        g.add(Ui.campo("Nombre completo", nombre), 0, 1, 2, 1);
        g.add(Ui.campo("Rol", rol), 0, 2);
        g.add(Ui.campo("Colegiatura", cop), 1, 2);
        g.add(Ui.campo("Correo", email), 0, 3, 2, 1);
        g.add(activo, 0, 4, 2, 1);

        notificador.dialogo(Feather.SHIELD, existente == null ? "Nuevo usuario" : "Editar usuario", g, "Guardar", false, () -> {
            u.setUsuario(login.getText());
            u.setNombre(nombre.getText());
            u.setEmail(email.getText() == null || email.getText().isBlank() ? null : email.getText().trim());
            u.setColegiatura(cop.getText() == null || cop.getText().isBlank() ? null : cop.getText().trim());
            u.setRol(rol.getValue());
            u.setActivo(activo.isSelected());
            servicio.guardar(u, clave.getText());
            notificador.exito("Usuario " + u.getUsuario() + " guardado.");
            cargar();
        }, 560);
    }

    private static <T> TableColumn<Usuario, T> col(String titulo, Function<Usuario, T> valor) {
        TableColumn<Usuario, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new SimpleObjectProperty<>(valor.apply(d.getValue())));
        c.setSortable(false);
        return c;
    }
}
