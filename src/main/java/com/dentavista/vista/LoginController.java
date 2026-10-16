package com.dentavista.vista;

import atlantafx.base.util.Animations;
import com.dentavista.JavaFxApplication;
import com.dentavista.servicio.DatosClinica;
import com.dentavista.servicio.NegocioException;
import com.dentavista.servicio.UsuarioService;
import com.dentavista.tresd.VisorDental3D;
import com.dentavista.ui.Ui;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

/** Pantalla de acceso con la dentadura 3D girando como presentación del producto. */
@Component
@RequiredArgsConstructor
public class LoginController {

    private final UsuarioService usuarios;
    private final DatosClinica clinica;

    @FXML private StackPane escenario;
    @FXML private VBox puntos;
    @FXML private Label lblClinica;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtClave;
    @FXML private Label lblError;
    @FXML private Button btnIngresar;

    private VisorDental3D visor;

    @FXML
    private void initialize() {
        lblClinica.setText("Ingresa a " + clinica.nombre() + ".");

        visor = new VisorDental3D();
        visor.numerosVisiblesProperty().set(false);
        visor.giroAutomaticoProperty().set(true);
        visor.setMouseTransparent(false);
        // El modelo deja libre la franja del título y la de los puntos destacados.
        javafx.scene.layout.StackPane.setMargin(visor, new javafx.geometry.Insets(120, 0, 150, 0));
        escenario.getChildren().add(0, visor);

        puntos.getChildren().addAll(
                punto(Feather.MOUSE_POINTER, "Odontograma 3D: clic en la cara exacta del diente"),
                punto(Feather.CALENDAR, "Agenda de citas y recordatorios por WhatsApp"),
                punto(Feather.DOLLAR_SIGN, "Presupuestos, pagos, recibos y recetas en PDF"),
                punto(Feather.USERS, "Tus pacientes ven su odontograma y piden citas"));
    }

    private HBox punto(Feather icono, String texto) {
        Label l = new Label(texto, Ui.icono(icono, 15));
        l.getStyleClass().add("login-punto");
        l.setGraphicTextGap(10);
        return new HBox(l);
    }

    public void alMostrar() {
        txtUsuario.requestFocus();
    }

    @FXML
    private void ingresar() {
        try {
            usuarios.autenticar(txtUsuario.getText(), txtClave.getText());
            visor.giroAutomaticoProperty().set(false);
            JavaFxApplication.get().mostrarPanel();
        } catch (NegocioException e) {
            lblError.setText(e.getMessage());
            lblError.setVisible(true);
            lblError.setManaged(true);
            Animations.shakeX(btnIngresar).play();
            txtClave.selectAll();
            txtClave.requestFocus();
        }
    }

    @FXML
    private void demoOdontologa() {
        rellenar("dra.paredes", "dentista123");
    }

    @FXML
    private void demoAdmin() {
        rellenar("admin", "admin123");
    }

    @FXML
    private void demoPaciente() {
        rellenar("72845163", "paciente123");
    }

    @FXML
    private void demoRecepcion() {
        rellenar("recepcion", "recepcion123");
    }

    private void rellenar(String u, String c) {
        txtUsuario.setText(u);
        txtClave.setText(c);
        btnIngresar.requestFocus();
    }
}
