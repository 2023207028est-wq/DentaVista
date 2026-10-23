package com.dentavista.vista;

import atlantafx.base.util.Animations;
import com.dentavista.JE_JE_SR_JA_ME_JavaFxApplication;
import com.dentavista.servicio.JE_JE_SR_JA_ME_DatosClinica;
import com.dentavista.servicio.JE_JE_SR_JA_ME_NegocioException;
import com.dentavista.servicio.JE_JE_SR_JA_ME_UsuarioService;
import com.dentavista.tresd.JE_JE_SR_JA_ME_VisorDental3D;
import com.dentavista.ui.JE_JE_SR_JA_ME_Ui;
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
public class JE_JE_SR_JA_ME_LoginController {

    private final JE_JE_SR_JA_ME_UsuarioService usuarios;
    private final JE_JE_SR_JA_ME_DatosClinica clinica;

    @FXML private StackPane escenario;
    @FXML private VBox puntos;
    @FXML private Label lblClinica;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtClave;
    @FXML private Label lblError;
    @FXML private Button btnIngresar;

    private JE_JE_SR_JA_ME_VisorDental3D visor;

    @FXML
    private void initialize() {
        lblClinica.setText("Ingresa a " + clinica.nombre() + ".");

        visor = new JE_JE_SR_JA_ME_VisorDental3D();
        visor.JE_JE_SR_JA_ME_numerosVisiblesProperty().set(false);
        visor.JE_JE_SR_JA_ME_giroAutomaticoProperty().set(true);
        visor.setMouseTransparent(false);
        // El modelo deja libre la franja del título y la de los puntos destacados.
        javafx.scene.layout.StackPane.setMargin(visor, new javafx.geometry.Insets(120, 0, 150, 0));
        escenario.getChildren().add(0, visor);

        puntos.getChildren().addAll(
                JE_JE_SR_JA_ME_punto(Feather.MOUSE_POINTER, "Odontograma 3D: clic en la cara exacta del diente"),
                JE_JE_SR_JA_ME_punto(Feather.CALENDAR, "Agenda de citas y recordatorios por WhatsApp"),
                JE_JE_SR_JA_ME_punto(Feather.DOLLAR_SIGN, "Presupuestos, pagos, recibos y recetas en PDF"),
                JE_JE_SR_JA_ME_punto(Feather.USERS, "Tus pacientes ven su odontograma y piden citas"));
    }

    private HBox JE_JE_SR_JA_ME_punto(Feather icono, String texto) {
        Label l = new Label(texto, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(icono, 15));
        l.getStyleClass().add("login-punto");
        l.setGraphicTextGap(10);
        return new HBox(l);
    }

    public void JE_JE_SR_JA_ME_alMostrar() {
        txtUsuario.requestFocus();
    }

    @FXML
    private void JE_JE_SR_JA_ME_ingresar() {
        try {
            usuarios.JE_JE_SR_JA_ME_autenticar(txtUsuario.getText(), txtClave.getText());
            visor.JE_JE_SR_JA_ME_giroAutomaticoProperty().set(false);
            JE_JE_SR_JA_ME_JavaFxApplication.JE_JE_SR_JA_ME_get().JE_JE_SR_JA_ME_mostrarPanel();
        } catch (JE_JE_SR_JA_ME_NegocioException e) {
            lblError.setText(e.getMessage());
            lblError.setVisible(true);
            lblError.setManaged(true);
            Animations.shakeX(btnIngresar).play();
            txtClave.selectAll();
            txtClave.requestFocus();
        }
    }

    @FXML
    private void JE_JE_SR_JA_ME_demoOdontologa() {
        JE_JE_SR_JA_ME_rellenar("dra.paredes", "dentista123");
    }

    @FXML
    private void JE_JE_SR_JA_ME_demoAdmin() {
        JE_JE_SR_JA_ME_rellenar("admin", "admin123");
    }

    @FXML
    private void JE_JE_SR_JA_ME_demoPaciente() {
        JE_JE_SR_JA_ME_rellenar("72845163", "paciente123");
    }

    @FXML
    private void JE_JE_SR_JA_ME_demoRecepcion() {
        JE_JE_SR_JA_ME_rellenar("recepcion", "recepcion123");
    }

    private void JE_JE_SR_JA_ME_rellenar(String u, String c) {
        txtUsuario.setText(u);
        txtClave.setText(c);
        btnIngresar.requestFocus();
    }
}
