package com.dentavista.vista;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.util.Animations;
import com.dentavista.JE_JE_SR_JA_ME_JavaFxApplication;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Usuario;
import com.dentavista.servicio.JE_JE_SR_JA_ME_DatosClinica;
import com.dentavista.servicio.JE_JE_SR_JA_ME_SesionService;
import com.dentavista.ui.JE_JE_SR_JA_ME_Notificador;
import com.dentavista.ui.JE_JE_SR_JA_ME_Tema;
import com.dentavista.ui.JE_JE_SR_JA_ME_Ui;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Panel principal: menú lateral, área de contenido, diálogos y notificaciones. */
@Component
@RequiredArgsConstructor
public class JE_JE_SR_JA_ME_MainController implements JE_JE_SR_JA_ME_Navegador {

    private final JE_JE_SR_JA_ME_SesionService sesion;
    private final JE_JE_SR_JA_ME_Tema tema;
    private final JE_JE_SR_JA_ME_Notificador notificador;
    private final JE_JE_SR_JA_ME_DatosClinica clinica;
    private final ObjectProvider<JE_JE_SR_JA_ME_InicioVista> inicio;
    private final ObjectProvider<JE_JE_SR_JA_ME_PacientesVista> pacientes;
    private final ObjectProvider<JE_JE_SR_JA_ME_FichaVista> fichas;
    private final ObjectProvider<JE_JE_SR_JA_ME_CatalogoVista> catalogo;
    private final ObjectProvider<JE_JE_SR_JA_ME_UsuariosVista> usuarios;
    private final ObjectProvider<JE_JE_SR_JA_ME_AgendaVista> agenda;
    private final ObjectProvider<JE_JE_SR_JA_ME_MisCitasVista> misCitas;
    private final com.dentavista.servicio.JE_JE_SR_JA_ME_CitaService citas;

    @FXML private StackPane raiz;
    @FXML private StackPane contenido;
    @FXML private VBox zonaToasts;
    @FXML private ImageView imgLogo;
    @FXML private ToggleButton navInicio;
    @FXML private ToggleButton navPacientes;
    @FXML private ToggleButton navTratamientos;
    @FXML private ToggleButton navUsuarios;
    @FXML private ToggleButton navAgenda;
    @FXML private ToggleButton navMiOdontograma;
    @FXML private ToggleButton navMisCitas;
    @FXML private Label secConsultorio;
    @FXML private Label secConfiguracion;
    @FXML private HBox tarjetaUsuario;
    @FXML private Label lblUsuario;
    @FXML private Label lblRol;
    @FXML private Label lblClinica;
    @FXML private Button btnTema;
    @FXML private Button btnSalir;

    private final ToggleGroup grupo = new ToggleGroup();
    private Map<JE_JE_SR_JA_ME_Seccion, ToggleButton> botones;
    private boolean navegando;

    @FXML
    private void initialize() {
        ModalPane modal = new ModalPane();
        modal.setId("modal");
        raiz.getChildren().add(1, modal);
        notificador.JE_JE_SR_JA_ME_registrar(modal, zonaToasts);

        var logo = getClass().getResourceAsStream("/img/logo.png");
        if (logo != null) {
            imgLogo.setImage(new Image(logo));
        }

        botones = Map.of(JE_JE_SR_JA_ME_Seccion.INICIO, navInicio, JE_JE_SR_JA_ME_Seccion.PACIENTES, navPacientes, JE_JE_SR_JA_ME_Seccion.AGENDA, navAgenda,
                JE_JE_SR_JA_ME_Seccion.TRATAMIENTOS, navTratamientos, JE_JE_SR_JA_ME_Seccion.USUARIOS, navUsuarios,
                JE_JE_SR_JA_ME_Seccion.MI_ODONTOGRAMA, navMiOdontograma, JE_JE_SR_JA_ME_Seccion.MIS_CITAS, navMisCitas);
        navAgenda.setGraphic(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.CALENDAR, 18));
        navMiOdontograma.setGraphic(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.SMILE, 18));
        navMisCitas.setGraphic(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.CALENDAR, 18));
        navInicio.setGraphic(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.HOME, 18));
        navPacientes.setGraphic(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.USERS, 18));
        navTratamientos.setGraphic(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.LAYERS, 18));
        navUsuarios.setGraphic(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.SHIELD, 18));
        botones.forEach((seccion, b) -> {
            b.setToggleGroup(grupo);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> JE_JE_SR_JA_ME_irA(seccion));
        });
        // Un botón del menú no se puede "deseleccionar" con un segundo clic.
        grupo.selectedToggleProperty().addListener((o, anterior, nuevo) -> {
            if (nuevo == null && anterior != null && !navegando) {
                anterior.setSelected(true);
            }
        });
        // El paciente solo ve su odontograma y sus citas; el personal, el consultorio.
        boolean paciente = sesion.JE_JE_SR_JA_ME_esPaciente();
        JE_JE_SR_JA_ME_mostrarNav(navUsuarios, sesion.JE_JE_SR_JA_ME_puedeAdministrar());
        for (Node n : new Node[]{navInicio, navPacientes, navAgenda, navTratamientos, secConfiguracion}) {
            JE_JE_SR_JA_ME_mostrarNav(n, !paciente);
        }
        JE_JE_SR_JA_ME_mostrarNav(navMiOdontograma, paciente);
        JE_JE_SR_JA_ME_mostrarNav(navMisCitas, paciente);
        secConsultorio.setText(paciente ? "MI SONRISA" : "CONSULTORIO");
        if (!paciente) {
            long porConfirmar = citas.JE_JE_SR_JA_ME_porConfirmar();
            if (porConfirmar > 0) {
                navAgenda.setText("Agenda (" + porConfirmar + ")");
                JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(navAgenda, porConfirmar + " solicitudes de pacientes por confirmar");
            }
        }

        JE_JE_SR_JA_ME_Usuario u = sesion.JE_JE_SR_JA_ME_getUsuario();
        tarjetaUsuario.getChildren().add(0, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_avatar(u.JE_JE_SR_JA_ME_iniciales(), u.getNombre(), 36));
        lblUsuario.setText(u.getNombre());
        lblRol.setText(u.getRol().JE_JE_SR_JA_ME_getEtiqueta() + (u.getColegiatura() != null ? " · " + u.getColegiatura() : "")
                + (paciente ? " · DNI " + u.getPaciente().getDni() : ""));
        lblClinica.setText(clinica.nombre());

        Runnable iconoTema = () -> {
            boolean oscuro = tema.JE_JE_SR_JA_ME_oscuroProperty().get();
            btnTema.setGraphic(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(oscuro ? Feather.SUN : Feather.MOON, 15));
            btnTema.setText(oscuro ? "Modo claro" : "Modo oscuro");
        };
        iconoTema.run();
        tema.JE_JE_SR_JA_ME_oscuroProperty().addListener((o, a, oscuro) -> iconoTema.run());
        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(btnTema, "Cambiar tema claro / oscuro");
        btnTema.setOnAction(e -> tema.JE_JE_SR_JA_ME_alternar());

        btnSalir.setGraphic(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.LOG_OUT, 15));
        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(btnSalir, "Cerrar sesión");
        btnSalir.setOnAction(e -> notificador.JE_JE_SR_JA_ME_confirmar("Cerrar sesión",
                "¿Quieres salir de DentaVista? Los cambios ya están guardados.", "Cerrar sesión", false, () -> {
                    sesion.JE_JE_SR_JA_ME_cerrar();
                    JE_JE_SR_JA_ME_JavaFxApplication.JE_JE_SR_JA_ME_get().JE_JE_SR_JA_ME_mostrarLogin();
                }));

        JE_JE_SR_JA_ME_irA(paciente ? JE_JE_SR_JA_ME_Seccion.MI_ODONTOGRAMA : JE_JE_SR_JA_ME_Seccion.INICIO);
    }

    private static void JE_JE_SR_JA_ME_mostrarNav(Node n, boolean visible) {
        n.setVisible(visible);
        n.setManaged(visible);
    }

    @Override
    public void JE_JE_SR_JA_ME_irA(JE_JE_SR_JA_ME_Seccion seccion) {
        JE_JE_SR_JA_ME_marcar(seccion);
        Node vista = switch (seccion) {
            case INICIO -> inicio.getObject().JE_JE_SR_JA_ME_vista();
            case PACIENTES -> pacientes.getObject().JE_JE_SR_JA_ME_vista();
            case TRATAMIENTOS -> catalogo.getObject().JE_JE_SR_JA_ME_vista();
            case USUARIOS -> usuarios.getObject().JE_JE_SR_JA_ME_vista();
            case AGENDA -> agenda.getObject().JE_JE_SR_JA_ME_vista();
            case MIS_CITAS -> misCitas.getObject().JE_JE_SR_JA_ME_vista();
            case MI_ODONTOGRAMA -> fichas.getObject().JE_JE_SR_JA_ME_vista(sesion.JE_JE_SR_JA_ME_pacientePropio());
        };
        JE_JE_SR_JA_ME_mostrar(vista);
    }

    @Override
    public void JE_JE_SR_JA_ME_abrirFicha(JE_JE_SR_JA_ME_Paciente paciente) {
        JE_JE_SR_JA_ME_marcar(sesion.JE_JE_SR_JA_ME_esPaciente() ? JE_JE_SR_JA_ME_Seccion.MI_ODONTOGRAMA : JE_JE_SR_JA_ME_Seccion.PACIENTES);
        JE_JE_SR_JA_ME_mostrar(fichas.getObject().JE_JE_SR_JA_ME_vista(paciente));
    }

    private void JE_JE_SR_JA_ME_marcar(JE_JE_SR_JA_ME_Seccion seccion) {
        navegando = true;
        botones.get(seccion).setSelected(true);
        navegando = false;
    }

    private void JE_JE_SR_JA_ME_mostrar(Node vista) {
        contenido.getChildren().setAll(vista);
        Animations.fadeIn(vista, Duration.millis(220)).play();
    }
}
