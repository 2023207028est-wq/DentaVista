package com.dentavista.vista;

import com.dentavista.JE_JE_SR_JA_ME_JavaFxApplication;
import com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Odontograma;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Pieza;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Consulta;
import com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoItem;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Hallazgo;
import com.dentavista.modelo.JE_JE_SR_JA_ME_ItemPlan;
import com.dentavista.modelo.JE_JE_SR_JA_ME_MedioPago;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Pago;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Tratamiento;
import com.dentavista.servicio.JE_JE_SR_JA_ME_CatalogoService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_ClinicaService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_NegocioException;
import com.dentavista.servicio.JE_JE_SR_JA_ME_PacienteService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_PagoService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_PdfService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_PlanService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_SesionService;
import com.dentavista.tresd.JE_JE_SR_JA_ME_VisorDental3D;
import com.dentavista.ui.JE_JE_SR_JA_ME_Capturas;
import com.dentavista.ui.JE_JE_SR_JA_ME_Formato;
import com.dentavista.ui.JE_JE_SR_JA_ME_Imagenes;
import com.dentavista.ui.JE_JE_SR_JA_ME_Notificador;
import com.dentavista.ui.JE_JE_SR_JA_ME_Odontograma2D;
import com.dentavista.ui.JE_JE_SR_JA_ME_Ui;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Ficha del paciente: el corazÃ³n de DentaVista.
 * <ul>
 *   <li><b>Odontograma</b>: visor 3D + odontograma 2D sincronizados, paleta de
 *       hallazgos, panel de la pieza seleccionada y lÃ­nea de tiempo.</li>
 *   <li><b>Plan y presupuesto</b>: tratamientos generados desde el odontograma,
 *       realizados / pendientes y PDF.</li>
 *   <li><b>Historial</b>: cada consulta con sus hallazgos y tratamientos.</li>
 * </ul>
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class JE_JE_SR_JA_ME_FichaVista {

    private final JE_JE_SR_JA_ME_ClinicaService clinica;
    private final JE_JE_SR_JA_ME_PlanService planes;
    private final JE_JE_SR_JA_ME_PacienteService pacientes;
    private final JE_JE_SR_JA_ME_CatalogoService catalogo;
    private final JE_JE_SR_JA_ME_SesionService sesion;
    private final JE_JE_SR_JA_ME_PdfService pdf;
    private final JE_JE_SR_JA_ME_Notificador notificador;
    private final JE_JE_SR_JA_ME_Navegador navegador;
    private final com.dentavista.servicio.JE_JE_SR_JA_ME_CitaService citas;
    private final com.dentavista.servicio.JE_JE_SR_JA_ME_UsuarioService usuarios;
    private final JE_JE_SR_JA_ME_DialogoCita dialogoCita;
    private final com.dentavista.servicio.JE_JE_SR_JA_ME_PagoService pagos;
    private final com.dentavista.servicio.JE_JE_SR_JA_ME_RecetaService recetas;
    private final JE_JE_SR_JA_ME_DialogoReceta dialogoReceta;

    private JE_JE_SR_JA_ME_Paciente paciente;
    private JE_JE_SR_JA_ME_Odontograma actual = JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_vacio();
    private Integer piezaSel;
    private JE_JE_SR_JA_ME_Superficie caraSel;
    /** Hallazgo que se pinta con cada clic; null = solo seleccionar. */
    private JE_JE_SR_JA_ME_EstadoDental herramienta;
    /** Fecha que se estÃ¡ viendo en la lÃ­nea de tiempo; null = hoy. */
    private LocalDateTime fechaVista;
    private List<JE_JE_SR_JA_ME_Consulta> cronologia = new ArrayList<>();

    private final BorderPane raiz = new BorderPane();
    private final StackPane cuerpo = new StackPane();
    private final HBox cabecera = new HBox();
    private JE_JE_SR_JA_ME_VisorDental3D visor;
    private JE_JE_SR_JA_ME_Odontograma2D o2d;
    private final VBox panelPieza = new VBox();
    private final Label estadoRegistro = new Label();
    private final ToggleGroup grupoHerramientas = new ToggleGroup();
    private final Map<JE_JE_SR_JA_ME_EstadoDental, ToggleButton> botonesHerramienta = new LinkedHashMap<>();
    private ToggleButton botonSeleccionar;
    private final Slider linea = new Slider();
    private final Label lblFecha = new Label();
    private final javafx.scene.layout.Pane marcas = new javafx.scene.layout.Pane();
    private final Label avisoHistorico = new Label();
    private Timeline reproduccion;
    private boolean ajustandoLinea;
    private final TextArea nota = new TextArea();
    /** Escenario 3D (visor + barra + lÃ­nea de tiempo); se traslada a una ventana propia en pantalla completa. */
    private StackPane escenario;
    private final ToggleButton botonPantallaCompleta = new ToggleButton(null, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.MAXIMIZE, 15));
    private javafx.stage.Stage ventanaCompleta;
    private javafx.event.EventHandler<KeyEvent> atajos;

    private Node pestanaOdontograma;
    private Node pestanaPlan;
    private Node pestanaHistorial;
    private final ToggleGroup grupoPestanas = new ToggleGroup();
    private ToggleButton tabOdontograma;

    private final TableView<JE_JE_SR_JA_ME_ItemPlan> tablaPlan = new TableView<>();
    private final VBox totalesPlan = new VBox();
    private final VBox panelCuenta = new VBox(14);
    private List<com.dentavista.modelo.JE_JE_SR_JA_ME_Receta> recetasDelPaciente = List.of();
    private final VBox listaHistorial = new VBox(0);

    public Node JE_JE_SR_JA_ME_vista(JE_JE_SR_JA_ME_Paciente p) {
        this.paciente = pacientes.JE_JE_SR_JA_ME_obtener(p.getId());

        JE_JE_SR_JA_ME_construirCabecera();
        pestanaOdontograma = JE_JE_SR_JA_ME_construirOdontograma();
        pestanaPlan = JE_JE_SR_JA_ME_construirPlan();
        pestanaHistorial = JE_JE_SR_JA_ME_construirHistorial();
        cuerpo.getChildren().setAll(pestanaOdontograma);

        raiz.setTop(cabecera);
        raiz.setCenter(cuerpo);
        if (!sesion.JE_JE_SR_JA_ME_esPaciente()) {
            JE_JE_SR_JA_ME_instalarAtajos();
        }

        JE_JE_SR_JA_ME_recargarTodo();
        JE_JE_SR_JA_ME_seleccionar(JE_JE_SR_JA_ME_primeraPiezaConHallazgo(), null);
        return raiz;
    }

    // =================================================================== cabecera

    private void JE_JE_SR_JA_ME_construirCabecera() {
        cabecera.getChildren().clear();
        cabecera.getStyleClass().setAll("cabecera-ficha");

        Button volver = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.ARROW_LEFT, "Volver a pacientes");
        volver.setOnAction(e -> navegador.JE_JE_SR_JA_ME_irA(JE_JE_SR_JA_ME_Navegador.JE_JE_SR_JA_ME_Seccion.PACIENTES));

        Label nombre = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(paciente.JE_JE_SR_JA_ME_nombreCompleto(), "ficha-nombre");
        List<String> datos = new ArrayList<>();
        datos.add("DNI " + paciente.getDni());
        if (paciente.JE_JE_SR_JA_ME_edad() != null) {
            datos.add(paciente.JE_JE_SR_JA_ME_edad() + " aÃ±os");
        }
        if (paciente.getSexo() != null) {
            datos.add(switch (paciente.getSexo()) {
                case "F" -> "Femenino";
                case "M" -> "Masculino";
                default -> "Otro";
            });
        }
        if (paciente.getTelefono() != null) {
            datos.add(paciente.getTelefono());
        }
        Label sub = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(String.join("  Â·  ", datos), "texto-tenue");
        HBox alertas = new HBox(6);
        alertas.setAlignment(Pos.CENTER_LEFT);
        if (!paciente.isActivo()) {
            alertas.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Archivado", "neutro", Feather.ARCHIVE));
        }
        if (paciente.getAlergias() != null) {
            Label alergia = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Alergia: " + paciente.getAlergias(), "peligro", Feather.ALERT_OCTAGON);
            // La alergia es informaciÃ³n crÃ­tica: nunca se recorta.
            alergia.setMinWidth(Region.USE_PREF_SIZE);
            alertas.getChildren().add(alergia);
        }
        if (paciente.getAntecedentes() != null) {
            Label a = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip(paciente.getAntecedentes(), "alerta", Feather.HEART);
            a.setMaxWidth(340);
            JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(a, paciente.getAntecedentes());
            alertas.getChildren().add(a);
        }
        nombre.setMinWidth(Region.USE_PREF_SIZE);
        HBox lineaSub = new HBox(10, sub);
        lineaSub.setAlignment(Pos.CENTER_LEFT);
        VBox textos = new VBox(3, new HBox(10, nombre, alertas), lineaSub);
        textos.setMinWidth(0);
        HBox.setHgrow(textos, Priority.ALWAYS);
        ((HBox) textos.getChildren().get(0)).setAlignment(Pos.CENTER_LEFT);

        tabOdontograma = JE_JE_SR_JA_ME_pestana("Odontograma 3D", Feather.BOX, () -> cuerpo.getChildren().setAll(pestanaOdontograma));
        ToggleButton tabPlan = JE_JE_SR_JA_ME_pestana("Plan y presupuesto", Feather.CLIPBOARD, () -> {
            JE_JE_SR_JA_ME_refrescarPlan();
            cuerpo.getChildren().setAll(pestanaPlan);
        });
        ToggleButton tabHist = JE_JE_SR_JA_ME_pestana("Historial", Feather.CLOCK, () -> {
            JE_JE_SR_JA_ME_refrescarHistorial();
            cuerpo.getChildren().setAll(pestanaHistorial);
        });
        tabOdontograma.setSelected(true);
        HBox pestanas = new HBox(tabOdontograma, tabPlan, tabHist);
        pestanas.getStyleClass().add("pestanas");

        Button editar = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.EDIT_2, "Editar datos del paciente");
        editar.setOnAction(e -> JE_JE_SR_JA_ME_FormularioPaciente.JE_JE_SR_JA_ME_abrir(notificador, pacientes, paciente, guardado -> {
            paciente = guardado;
            JE_JE_SR_JA_ME_construirCabecera();
        }));
        Button exportar = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_primario("Informe clÃ­nico", Feather.DOWNLOAD);
        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(exportar, "PDF con el odontograma 3D, los hallazgos y las consultas (el presupuesto estÃ¡ en su pestaÃ±a)");
        exportar.setOnAction(e -> JE_JE_SR_JA_ME_exportarInforme());

        boolean esPaciente = sesion.JE_JE_SR_JA_ME_esPaciente();
        Button cita = esPaciente ? JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("Pedir cita", Feather.CALENDAR)
                : JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.CALENDAR, "Agendar cita");
        cita.setOnAction(e -> dialogoCita.JE_JE_SR_JA_ME_abrir(new JE_JE_SR_JA_ME_DialogoCita.JE_JE_SR_JA_ME_Sugerencia(paciente, null, null, null), c -> JE_JE_SR_JA_ME_construirCabecera()));
        citas.JE_JE_SR_JA_ME_proxima(paciente).ifPresent(c -> {
            Label chip = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Cita " + c.getInicio().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                    + (c.getEstado() == com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita.SOLICITADA ? " (por confirmar)" : ""),
                    "info", Feather.CALENDAR);
            JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(chip, "PrÃ³xima cita: " + com.dentavista.ui.JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_fechaHora(c.getInicio()) + " con "
                    + c.getOdontologo().getNombre());
            chip.setMinWidth(Region.USE_PREF_SIZE);
            lineaSub.getChildren().add(chip);
        });

        if (esPaciente) {
            cabecera.getChildren().addAll(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_avatar(paciente.JE_JE_SR_JA_ME_iniciales(), paciente.getDni(), 46), textos,
                    pestanas, cita, exportar);
            JE_JE_SR_JA_ME_fijarAncho(pestanas, cita, exportar);
            return;
        }
        Button receta = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.FILE_PLUS, "Emitir receta (verifica alergias y antecedentes)");
        receta.setOnAction(e -> dialogoReceta.JE_JE_SR_JA_ME_abrir(paciente, () -> {
            if (pestanaHistorial != null && cuerpo.getChildren().contains(pestanaHistorial)) {
                JE_JE_SR_JA_ME_refrescarHistorial();
            }
        }));
        receta.setVisible(sesion.JE_JE_SR_JA_ME_puedeRegistrarClinica());
        receta.setManaged(sesion.JE_JE_SR_JA_ME_puedeRegistrarClinica());
        Button acceso = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.KEY, "Acceso del paciente (usuario para ver su odontograma y pedir citas)");
        acceso.setOnAction(e -> JE_JE_SR_JA_ME_dialogoAcceso());
        Button baja = paciente.isActivo() ? JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.ARCHIVE, "Archivar (dar de baja): ya no necesita atenciÃ³n")
                : JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.ROTATE_CCW, "Reactivar paciente");
        baja.setOnAction(e -> JE_JE_SR_JA_ME_archivarOReactivar());
        cabecera.getChildren().addAll(volver, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_avatar(paciente.JE_JE_SR_JA_ME_iniciales(), paciente.getDni(), 46), textos,
                pestanas, acceso, editar, baja, receta, cita, exportar);
        JE_JE_SR_JA_ME_fijarAncho(pestanas, acceso, editar, baja, receta, cita, exportar);
    }

    private void JE_JE_SR_JA_ME_archivarOReactivar() {
        if (!paciente.isActivo()) {
            if (notificador.JE_JE_SR_JA_ME_intentar(() -> paciente = pacientes.JE_JE_SR_JA_ME_reactivar(paciente))) {
                notificador.JE_JE_SR_JA_ME_exito(paciente.JE_JE_SR_JA_ME_nombreCompleto() + " vuelve a estar activo.");
                JE_JE_SR_JA_ME_construirCabecera();
            }
            return;
        }
        Label info = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(paciente.JE_JE_SR_JA_ME_nombreCompleto() + " dejarÃ¡ de aparecer en la lista, la agenda y los avisos. "
                + "Su historia clÃ­nica se conserva y puedes reactivarlo cuando vuelva. "
                + "Sus citas por venir se cancelan y su acceso queda desactivado.", "texto-tenue");
        info.setWrapText(true);
        notificador.JE_JE_SR_JA_ME_dialogo(Feather.ARCHIVE, "Archivar paciente", new VBox(info), "Archivar", true, () -> {
            pacientes.JE_JE_SR_JA_ME_archivar(paciente);
            notificador.JE_JE_SR_JA_ME_exito(paciente.JE_JE_SR_JA_ME_nombreCompleto() + " fue archivado.");
            navegador.JE_JE_SR_JA_ME_irA(JE_JE_SR_JA_ME_Navegador.JE_JE_SR_JA_ME_Seccion.PACIENTES);
        }, 480);
    }

    /** Botones y pestaÃ±as nunca se recortan: si falta espacio se encoge el nombre del paciente. */
    private static void JE_JE_SR_JA_ME_fijarAncho(Region... nodos) {
        for (Region r : nodos) {
            r.setMinWidth(Region.USE_PREF_SIZE);
            if (r instanceof HBox h) {
                h.getChildren().forEach(n -> ((Region) n).setMinWidth(Region.USE_PREF_SIZE));
            }
        }
    }

    private ToggleButton JE_JE_SR_JA_ME_pestana(String texto, Feather icono, Runnable accion) {
        ToggleButton t = new ToggleButton(texto, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(icono, 15));
        t.setToggleGroup(grupoPestanas);
        t.setOnAction(e -> {
            if (!t.isSelected()) {
                t.setSelected(true);
            }
            accion.run();
        });
        return t;
    }

    // ============================================================ odontograma

    private Node JE_JE_SR_JA_ME_construirOdontograma() {
        visor = new JE_JE_SR_JA_ME_VisorDental3D();
        visor.JE_JE_SR_JA_ME_setAlPulsar(this::JE_JE_SR_JA_ME_alPulsarPieza);
        visor.JE_JE_SR_JA_ME_setAlDobleClic(this::JE_JE_SR_JA_ME_volverSana);

        o2d = new JE_JE_SR_JA_ME_Odontograma2D();
        o2d.JE_JE_SR_JA_ME_setAlPulsar(this::JE_JE_SR_JA_ME_alPulsarPieza);
        o2d.JE_JE_SR_JA_ME_setAlDobleClic(this::JE_JE_SR_JA_ME_volverSana);
        o2d.setPrefHeight(210);
        o2d.setMinHeight(150);

        escenario = new StackPane(visor, JE_JE_SR_JA_ME_barra3D(), JE_JE_SR_JA_ME_lineaTiempo(), JE_JE_SR_JA_ME_aviso());
        escenario.getStyleClass().add("escenario-3d");
        escenario.setMinHeight(300);
        VBox.setVgrow(escenario, Priority.ALWAYS);
        // Recorta el 3D a las esquinas redondeadas del escenario.
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        clip.widthProperty().bind(escenario.widthProperty());
        clip.heightProperty().bind(escenario.heightProperty());
        escenario.setClip(clip);

        VBox tarjeta2d = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tarjeta(o2d);
        tarjeta2d.setPadding(new Insets(8, 12, 8, 12));

        VBox centro = new VBox(12, escenario, tarjeta2d);
        HBox.setHgrow(centro, Priority.ALWAYS);

        ScrollPane scrollPieza = new ScrollPane(panelPieza);
        scrollPieza.setFitToWidth(true);
        scrollPieza.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPieza.setMinWidth(292);
        scrollPieza.setPrefWidth(292);
        panelPieza.getStyleClass().add("panel-pieza");

        // El paciente ve su boca en modo consulta: sin paleta de hallazgos.
        HBox h = sesion.JE_JE_SR_JA_ME_esPaciente() ? new HBox(14, centro, scrollPieza) : new HBox(14, JE_JE_SR_JA_ME_paleta(), centro, scrollPieza);
        h.setPadding(new Insets(14, 16, 14, 16));
        return h;
    }

    /** Paleta de hallazgos: elegir uno y hacer clic en la cara del diente. */
    private Node JE_JE_SR_JA_ME_paleta() {
        VBox v = new VBox(3);
        v.getStyleClass().add("paleta");
        v.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("HERRAMIENTA", "campo-rotulo"));

        botonSeleccionar = new ToggleButton("Seleccionar", JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.MOUSE_POINTER, 15));
        botonSeleccionar.setToggleGroup(grupoHerramientas);
        botonSeleccionar.setSelected(true);
        botonSeleccionar.setUserData(null);
        v.getChildren().add(JE_JE_SR_JA_ME_conAtajo(botonSeleccionar, "Esc"));

        v.getChildren().add(JE_JE_SR_JA_ME_separadorPaleta("EN UNA CARA"));
        int tecla = 1;
        for (JE_JE_SR_JA_ME_EstadoDental e : JE_JE_SR_JA_ME_EstadoDental.values()) {
            if (e.JE_JE_SR_JA_ME_seAplicaACara() && e != JE_JE_SR_JA_ME_EstadoDental.SANO) {
                v.getChildren().add(JE_JE_SR_JA_ME_botonHerramienta(e, tecla++));
            }
        }
        v.getChildren().add(JE_JE_SR_JA_ME_separadorPaleta("PIEZA COMPLETA"));
        for (JE_JE_SR_JA_ME_EstadoDental e : JE_JE_SR_JA_ME_EstadoDental.values()) {
            if (e.JE_JE_SR_JA_ME_getAlcance() == JE_JE_SR_JA_ME_EstadoDental.JE_JE_SR_JA_ME_Alcance.PIEZA) {
                v.getChildren().add(JE_JE_SR_JA_ME_botonHerramienta(e, tecla <= 9 ? tecla++ : -1));
            }
        }
        v.getChildren().add(JE_JE_SR_JA_ME_separadorPaleta("CORREGIR"));
        v.getChildren().add(JE_JE_SR_JA_ME_botonHerramienta(JE_JE_SR_JA_ME_EstadoDental.SANO, 0));

        grupoHerramientas.selectedToggleProperty().addListener((o, a, nuevo) -> {
            if (nuevo == null) {
                botonSeleccionar.setSelected(true);
                return;
            }
            herramienta = (JE_JE_SR_JA_ME_EstadoDental) nuevo.getUserData();
            visor.JE_JE_SR_JA_ME_setResaltarCara(herramienta == null || herramienta.JE_JE_SR_JA_ME_seAplicaACara());
        });

        Button deshacer = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("Deshacer", Feather.ROTATE_CCW);
        deshacer.setMaxWidth(Double.MAX_VALUE);
        deshacer.setOnAction(e -> JE_JE_SR_JA_ME_deshacer());
        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(deshacer, "Deshace el Ãºltimo registro de hoy (Ctrl+Z)");

        estadoRegistro.getStyleClass().addAll("texto-tenue", "texto-pequeno");
        estadoRegistro.setWrapText(true);
        estadoRegistro.setText("Elige un hallazgo y haz clic en la cara del diente, en 3D o en el odontograma. "
                + "Doble clic en una cara la vuelve sana.");

        Region esp = new Region();
        VBox.setVgrow(esp, Priority.ALWAYS);
        v.getChildren().addAll(esp, deshacer, estadoRegistro);

        if (!sesion.JE_JE_SR_JA_ME_puedeRegistrarClinica()) {
            botonesHerramienta.values().forEach(b -> b.setDisable(true));
            deshacer.setDisable(true);
            estadoRegistro.setText("Modo consulta: tu rol puede ver el odontograma pero no registrar hallazgos.");
        }
        ScrollPane s = new ScrollPane(v);
        s.setFitToWidth(true);
        s.setFitToHeight(true);
        s.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        s.setMinWidth(208);
        return s;
    }

    private Label JE_JE_SR_JA_ME_separadorPaleta(String texto) {
        Label l = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(texto, "campo-rotulo");
        l.setPadding(new Insets(10, 0, 2, 4));
        l.setStyle("-fx-font-size: 10px;");
        return l;
    }

    private Node JE_JE_SR_JA_ME_botonHerramienta(JE_JE_SR_JA_ME_EstadoDental e, int tecla) {
        ToggleButton b = new ToggleButton(e.JE_JE_SR_JA_ME_getEtiqueta(), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_muestra(e.JE_JE_SR_JA_ME_getColor()));
        b.setToggleGroup(grupoHerramientas);
        b.setUserData(e);
        botonesHerramienta.put(e, b);
        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(b, JE_JE_SR_JA_ME_ayudaEstado(e) + (tecla >= 0 ? "  (tecla " + tecla + ")" : ""));
        return JE_JE_SR_JA_ME_conAtajo(b, tecla >= 0 ? String.valueOf(tecla) : "");
    }

    private Node JE_JE_SR_JA_ME_conAtajo(ToggleButton b, String atajo) {
        Label l = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(atajo, "atajo");
        l.setMouseTransparent(true);
        StackPane s = new StackPane(b, l);
        StackPane.setAlignment(l, Pos.CENTER_RIGHT);
        StackPane.setMargin(l, new Insets(0, 10, 0, 0));
        return s;
    }

    private static String JE_JE_SR_JA_ME_ayudaEstado(JE_JE_SR_JA_ME_EstadoDental e) {
        return switch (e) {
            case SANO -> "Borra el hallazgo de la cara pulsada (o de toda la pieza si pulsas la silueta)";
            case CARIES -> "LesiÃ³n cariosa en la cara pulsada";
            case FRACTURA -> "Fractura o fisura en la cara pulsada";
            case OBTURADO -> "RestauraciÃ³n existente con resina";
            case SELLANTE -> "Sellante de fosas y fisuras";
            case ENDODONCIA_INDICADA -> "La pieza necesita tratamiento de conductos";
            case ENDODONCIA -> "Pieza con tratamiento de conductos";
            case EXTRACCION_INDICADA -> "La pieza debe extraerse";
            case CORONA -> "Pieza con corona protÃ©sica";
            case IMPLANTE -> "Implante dental en la posiciÃ³n";
            case AUSENTE -> "Pieza ausente o extraÃ­da";
        };
    }

    /** Barra flotante sobre el 3D: vistas, apertura de boca y opciones. */
    private Node JE_JE_SR_JA_ME_barra3D() {
        Button frontal = JE_JE_SR_JA_ME_boton3D("Frontal", Feather.SMILE, visor::JE_JE_SR_JA_ME_vistaFrontal);
        Button sup = JE_JE_SR_JA_ME_boton3D("Superior", Feather.CHEVRONS_UP, visor::JE_JE_SR_JA_ME_vistaSuperior);
        Button inf = JE_JE_SR_JA_ME_boton3D("Inferior", Feather.CHEVRONS_DOWN, visor::JE_JE_SR_JA_ME_vistaInferior);
        Button der = JE_JE_SR_JA_ME_boton3D("Derecha", Feather.CHEVRON_LEFT, visor::JE_JE_SR_JA_ME_vistaDerecha);
        Button izq = JE_JE_SR_JA_ME_boton3D("Izquierda", Feather.CHEVRON_RIGHT, visor::JE_JE_SR_JA_ME_vistaIzquierda);
        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(der, "Lado derecho del paciente");
        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(izq, "Lado izquierdo del paciente");

        ToggleButton abrir = JE_JE_SR_JA_ME_toggle3D("Abrir boca", Feather.MAXIMIZE_2, visor.JE_JE_SR_JA_ME_bocaAbiertaProperty());
        ToggleButton numeros = JE_JE_SR_JA_ME_toggle3D(null, Feather.HASH, visor.JE_JE_SR_JA_ME_numerosVisiblesProperty());
        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(numeros, "Mostrar nÃºmeros FDI");
        ToggleButton encias = JE_JE_SR_JA_ME_toggle3D(null, Feather.LAYERS, visor.JE_JE_SR_JA_ME_enciasVisiblesProperty());
        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(encias, "Mostrar encÃ­as");
        ToggleButton giro = JE_JE_SR_JA_ME_toggle3D(null, Feather.REFRESH_CW, visor.JE_JE_SR_JA_ME_giroAutomaticoProperty());
        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(giro, "Giro automÃ¡tico (modo presentaciÃ³n al paciente)");
        Button enfocar = JE_JE_SR_JA_ME_boton3D(null, Feather.CROSSHAIR, () -> {
            if (piezaSel != null) {
                visor.JE_JE_SR_JA_ME_enfocar(piezaSel);
            }
        });
        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(enfocar, "Acercar a la pieza seleccionada");
        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(botonPantallaCompleta, "Pantalla completa (F11) Â· Esc para salir");
        botonPantallaCompleta.setOnAction(e -> JE_JE_SR_JA_ME_pantallaCompleta(botonPantallaCompleta.isSelected()));

        HBox h = new HBox(frontal, sup, inf, der, izq, JE_JE_SR_JA_ME_sep(), abrir, JE_JE_SR_JA_ME_sep(), numeros, encias, giro, enfocar, JE_JE_SR_JA_ME_sep(),
                botonPantallaCompleta);
        h.getStyleClass().add("barra-3d");
        h.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(h, Pos.TOP_CENTER);
        StackPane.setMargin(h, new Insets(12, 0, 0, 0));

        Label ayuda = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Arrastra para girar Â· rueda para acercar Â· clic derecho para mover", "texto-pequeno");
        ayuda.setStyle("-fx-text-fill: rgba(203,213,225,0.55);");
        VBox caja = new VBox(6, h, ayuda);
        caja.setAlignment(Pos.TOP_CENTER);
        caja.setPickOnBounds(false);
        caja.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(caja, Pos.TOP_CENTER);
        StackPane.setMargin(caja, new Insets(12, 0, 0, 0));
        return caja;
    }

    private Button JE_JE_SR_JA_ME_boton3D(String texto, Feather icono, Runnable accion) {
        Button b = new Button(texto, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(icono, 15));
        b.setOnAction(e -> accion.run());
        return b;
    }

    private ToggleButton JE_JE_SR_JA_ME_toggle3D(String texto, Feather icono, javafx.beans.property.BooleanProperty prop) {
        ToggleButton t = new ToggleButton(texto, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(icono, 15));
        t.selectedProperty().bindBidirectional(prop);
        return t;
    }

    private Region JE_JE_SR_JA_ME_sep() {
        Region r = new Region();
        r.getStyleClass().add("separador");
        HBox.setMargin(r, new Insets(0, 4, 0, 4));
        return r;
    }

    private Node JE_JE_SR_JA_ME_aviso() {
        avisoHistorico.setGraphic(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.CLOCK, 14));
        avisoHistorico.getStyleClass().add("aviso-historico");
        avisoHistorico.setVisible(false);
        StackPane.setAlignment(avisoHistorico, Pos.TOP_CENTER);
        StackPane.setMargin(avisoHistorico, new Insets(86, 0, 0, 0));
        avisoHistorico.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return avisoHistorico;
    }

    // ------------------------------------------------------------ lÃ­nea de tiempo

    /**
     * "MÃ¡quina del tiempo": cada marca es una consulta; al moverla, el odontograma
     * muestra la boca tal como estaba al terminar esa consulta. El botÃ³n â–¶ reproduce
     * la evoluciÃ³n completa del paciente.
     */
    private Node JE_JE_SR_JA_ME_lineaTiempo() {
        Button play = new Button(null, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.PLAY, 14));
        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(play, "Reproducir la evoluciÃ³n del paciente");
        play.setOnAction(e -> JE_JE_SR_JA_ME_reproducir(play));

        linea.setMin(0);
        linea.setMajorTickUnit(1);
        linea.setMinorTickCount(0);
        linea.setSnapToTicks(true);
        linea.setShowTickMarks(true);
        linea.setShowTickLabels(false);
        linea.setBlockIncrement(1);
        HBox.setHgrow(linea, Priority.ALWAYS);
        linea.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double d) {
                int i = (int) Math.round(d);
                if (i >= cronologia.size()) {
                    return "Hoy";
                }
                return JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_mesCorto(cronologia.get(i).getFecha());
            }

            @Override
            public Double fromString(String s) {
                return 0.0;
            }
        });
        linea.valueProperty().addListener((o, a, v) -> {
            if (!ajustandoLinea) {
                JE_JE_SR_JA_ME_irAPunto((int) Math.round(v.doubleValue()));
            }
        });

        lblFecha.getStyleClass().add("fecha");
        lblFecha.setMinWidth(300);
        lblFecha.setPrefWidth(300);
        lblFecha.setMaxWidth(300);
        Label rotulo = new Label("LÃNEA DE TIEMPO", JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.CLOCK, 13));
        rotulo.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");

        // Marcas con el mes de cada consulta, alineadas con las posiciones del deslizador.
        marcas.setMinHeight(16);
        marcas.setPrefHeight(16);
        marcas.widthProperty().addListener((o, a, b) -> JE_JE_SR_JA_ME_dibujarMarcas());
        VBox deslizador = new VBox(2, linea, marcas);
        deslizador.setAlignment(Pos.CENTER);
        HBox.setHgrow(deslizador, Priority.ALWAYS);
        HBox h = new HBox(10, play, new VBox(1, rotulo, lblFecha), deslizador);
        h.getStyleClass().add("linea-tiempo");
        h.setAlignment(Pos.CENTER_LEFT);
        h.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(h, Pos.BOTTOM_CENTER);
        StackPane.setMargin(h, new Insets(0, 16, 14, 16));
        return h;
    }

    private void JE_JE_SR_JA_ME_dibujarMarcas() {
        marcas.getChildren().clear();
        int n = cronologia.size();
        double w = marcas.getWidth();
        if (n == 0 || w <= 0) {
            return;
        }
        // El pulgar del deslizador mide ~16 px: los extremos quedan a 8 px del borde.
        double margen = 9;
        for (int i = 0; i <= n; i++) {
            Label l = new Label(i == n ? "Hoy" : JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_mesCorto(cronologia.get(i).getFecha()));
            l.setStyle("-fx-font-size: 10px; -fx-text-fill: #7c8aa3;");
            double centro = margen + (w - 2 * margen) * i / n;
            // El Pane da a cada etiqueta su tamaÃ±o preferido; se centra cuando ya lo conoce.
            l.layoutXProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                    () -> Math.max(0, Math.min(w - l.getWidth(), centro - l.getWidth() / 2)), l.widthProperty()));
            marcas.getChildren().add(l);
        }
    }

    private void JE_JE_SR_JA_ME_configurarLinea() {
        ajustandoLinea = true;
        int n = cronologia.size();
        linea.setMax(Math.max(1, n));
        linea.setValue(n);
        linea.setDisable(n == 0);
        ajustandoLinea = false;
        JE_JE_SR_JA_ME_actualizarRotuloFecha(n);
        JE_JE_SR_JA_ME_dibujarMarcas();
    }

    private void JE_JE_SR_JA_ME_irAPunto(int i) {
        int n = cronologia.size();
        if (i >= n) {
            fechaVista = null;
        } else if (i + 1 < n) {
            // Estado al cerrar la consulta i: todo lo registrado antes de la siguiente.
            fechaVista = cronologia.get(i + 1).getFecha().minusSeconds(1);
        } else {
            fechaVista = cronologia.get(i).getFecha().plusHours(12);
        }
        JE_JE_SR_JA_ME_actualizarRotuloFecha(i);
        JE_JE_SR_JA_ME_refrescarOdontograma();
    }

    private void JE_JE_SR_JA_ME_actualizarRotuloFecha(int i) {
        int n = cronologia.size();
        if (i >= n) {
            lblFecha.setText("Hoy Â· estado actual");
            avisoHistorico.setVisible(false);
        } else {
            JE_JE_SR_JA_ME_Consulta c = cronologia.get(i);
            lblFecha.setText(JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_fecha(c.getFecha()) + " Â· " + c.getMotivo());
            avisoHistorico.setText("Viendo el " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_fecha(c.getFecha()) + " (consulta " + (i + 1) + " de " + n
                    + ") Â· solo lectura");
            avisoHistorico.setVisible(true);
        }
    }

    private void JE_JE_SR_JA_ME_reproducir(Button play) {
        if (reproduccion != null) {
            reproduccion.stop();
            reproduccion = null;
            play.setGraphic(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.PLAY, 14));
            return;
        }
        int n = cronologia.size();
        if (n == 0) {
            notificador.JE_JE_SR_JA_ME_info("Este paciente aÃºn no tiene consultas registradas.");
            return;
        }
        play.setGraphic(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.PAUSE, 14));
        visor.JE_JE_SR_JA_ME_vistaFrontal();
        reproduccion = new Timeline();
        for (int i = 0; i <= n; i++) {
            final int punto = i;
            reproduccion.getKeyFrames().add(new KeyFrame(Duration.millis(300 + i * 1300L), e -> linea.setValue(punto)));
        }
        reproduccion.setOnFinished(e -> {
            reproduccion = null;
            play.setGraphic(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.PLAY, 14));
        });
        reproduccion.play();
    }

    // ------------------------------------------------------------- interacciÃ³n

    private void JE_JE_SR_JA_ME_alPulsarPieza(Integer fdi, JE_JE_SR_JA_ME_Superficie cara) {
        if (fdi == null) {
            return;
        }
        JE_JE_SR_JA_ME_seleccionar(fdi, cara);
        if (herramienta == null) {
            return;
        }
        if (fechaVista != null) {
            notificador.JE_JE_SR_JA_ME_info("EstÃ¡s viendo una fecha pasada. Mueve la lÃ­nea de tiempo a \"Hoy\" para registrar.");
            return;
        }
        JE_JE_SR_JA_ME_aplicar(fdi, cara, herramienta);
    }

    /**
     * Doble clic: la cara pulsada vuelve a estar sana (o la pieza completa si se pulsÃ³ la
     * silueta o si la pieza estaba ausente, con corona o implante). Si el primer clic del
     * doble clic acaba de registrar un hallazgo con la herramienta activa, ese registro se
     * deshace en lugar de dejar "caries" y "sano" seguidos en la historia.
     */
    private void JE_JE_SR_JA_ME_volverSana(Integer fdi, JE_JE_SR_JA_ME_Superficie cara) {
        if (fdi == null) {
            return;
        }
        if (!sesion.JE_JE_SR_JA_ME_puedeRegistrarClinica()) {
            notificador.JE_JE_SR_JA_ME_info("Tu rol solo puede consultar el odontograma.");
            return;
        }
        if (fechaVista != null) {
            notificador.JE_JE_SR_JA_ME_info("EstÃ¡s viendo una fecha pasada. Mueve la lÃ­nea de tiempo a \"Hoy\" para registrar.");
            return;
        }
        JE_JE_SR_JA_ME_seleccionar(fdi, cara);
        if (ultimoClic != null && ultimoClic.getPieza() == fdi && System.nanoTime() - ultimoClicNanos < 800_000_000L) {
            notificador.JE_JE_SR_JA_ME_intentar(() -> clinica.JE_JE_SR_JA_ME_deshacerUltimo(paciente));
        }
        ultimoClic = null;
        JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_EstadoPieza estado = clinica.JE_JE_SR_JA_ME_odontograma(paciente).JE_JE_SR_JA_ME_pieza(fdi);
        boolean piezaCompleta = cara == JE_JE_SR_JA_ME_Superficie.PIEZA
                || (estado.JE_JE_SR_JA_ME_general() != null && estado.JE_JE_SR_JA_ME_general().JE_JE_SR_JA_ME_anulaCaras());
        JE_JE_SR_JA_ME_Superficie destino = piezaCompleta ? JE_JE_SR_JA_ME_Superficie.PIEZA : cara;
        JE_JE_SR_JA_ME_aplicar(fdi, destino, JE_JE_SR_JA_ME_EstadoDental.SANO);
        if (estadoRegistro.getText().startsWith("Sin cambios")) {
            estadoRegistro.setText("La " + (piezaCompleta ? "pieza " + fdi : "cara " + destino.JE_JE_SR_JA_ME_etiqueta(fdi).toLowerCase()
                    + " de la pieza " + fdi) + " ya estÃ¡ sana.");
            cronologia = clinica.JE_JE_SR_JA_ME_consultasCronologicas(paciente);
            JE_JE_SR_JA_ME_configurarLinea();
            JE_JE_SR_JA_ME_refrescarOdontograma();
        }
    }

    private JE_JE_SR_JA_ME_Hallazgo ultimoClic;
    private long ultimoClicNanos;

    private void JE_JE_SR_JA_ME_aplicar(int fdi, JE_JE_SR_JA_ME_Superficie cara, JE_JE_SR_JA_ME_EstadoDental estado) {
        JE_JE_SR_JA_ME_Superficie destino;
        if (estado == JE_JE_SR_JA_ME_EstadoDental.SANO) {
            destino = cara;
        } else if (estado.JE_JE_SR_JA_ME_getAlcance() == JE_JE_SR_JA_ME_EstadoDental.JE_JE_SR_JA_ME_Alcance.PIEZA) {
            destino = JE_JE_SR_JA_ME_Superficie.PIEZA;
        } else if (cara == JE_JE_SR_JA_ME_Superficie.PIEZA) {
            notificador.JE_JE_SR_JA_ME_advertencia(estado.JE_JE_SR_JA_ME_getEtiqueta() + " se marca en una cara: haz clic en el cuadro de caras o en la cara del diente 3D.");
            return;
        } else {
            destino = cara;
        }
        try {
            JE_JE_SR_JA_ME_Hallazgo h = clinica.JE_JE_SR_JA_ME_registrar(paciente, fdi, destino, estado, nota.getText());
            if (h == null) {
                estadoRegistro.setText("Sin cambios: " + fdi + " " + destino.JE_JE_SR_JA_ME_etiqueta(fdi).toLowerCase()
                        + " ya estaba como " + estado.JE_JE_SR_JA_ME_getEtiqueta().toLowerCase() + ".");
                return;
            }
            nota.clear();
            ultimoClic = estado == JE_JE_SR_JA_ME_EstadoDental.SANO ? null : h;
            ultimoClicNanos = System.nanoTime();
            estadoRegistro.setText("âœ” Registrado: " + estado.JE_JE_SR_JA_ME_getEtiqueta() + " Â· pieza " + fdi
                    + (destino.JE_JE_SR_JA_ME_esCara() ? " " + destino.JE_JE_SR_JA_ME_etiqueta(fdi).toLowerCase() : ""));
            boolean nuevaConsulta = cronologia.stream().noneMatch(c -> c.getId().equals(h.getConsulta().getId()));
            if (nuevaConsulta) {
                cronologia = clinica.JE_JE_SR_JA_ME_consultasCronologicas(paciente);
                JE_JE_SR_JA_ME_configurarLinea();
            }
            JE_JE_SR_JA_ME_refrescarOdontograma();
        } catch (JE_JE_SR_JA_ME_NegocioException e) {
            notificador.JE_JE_SR_JA_ME_advertencia(e.getMessage());
        }
    }

    private void JE_JE_SR_JA_ME_deshacer() {
        notificador.JE_JE_SR_JA_ME_intentar(() -> {
            JE_JE_SR_JA_ME_Hallazgo h = clinica.JE_JE_SR_JA_ME_deshacerUltimo(paciente);
            if (h == null) {
                notificador.JE_JE_SR_JA_ME_info("No hay registros de hoy para deshacer.");
                return;
            }
            estadoRegistro.setText("â†¶ Deshecho: " + h.getEstado().JE_JE_SR_JA_ME_getEtiqueta() + " Â· pieza " + h.getPieza());
            cronologia = clinica.JE_JE_SR_JA_ME_consultasCronologicas(paciente);
            JE_JE_SR_JA_ME_configurarLinea();
            JE_JE_SR_JA_ME_seleccionar(h.getPieza(), h.getSuperficie() == JE_JE_SR_JA_ME_Superficie.PIEZA ? null : h.getSuperficie());
            JE_JE_SR_JA_ME_refrescarOdontograma();
        });
    }

    private void JE_JE_SR_JA_ME_seleccionar(Integer fdi, JE_JE_SR_JA_ME_Superficie cara) {
        piezaSel = fdi;
        caraSel = cara == JE_JE_SR_JA_ME_Superficie.PIEZA ? null : cara;
        visor.JE_JE_SR_JA_ME_seleccionar(fdi);
        o2d.JE_JE_SR_JA_ME_seleccionar(fdi);
        JE_JE_SR_JA_ME_refrescarPanelPieza();
    }

    private Integer JE_JE_SR_JA_ME_primeraPiezaConHallazgo() {
        return actual.JE_JE_SR_JA_ME_patologias().stream().map(JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_Patologia::pieza).findFirst().orElse(16);
    }

    private void JE_JE_SR_JA_ME_instalarAtajos() {
        atajos = e -> {
            if (e.getTarget() instanceof TextInputControl || !tabOdontograma.isSelected()) {
                return;
            }
            if (e.getCode() == KeyCode.F11) {
                JE_JE_SR_JA_ME_pantallaCompleta(ventanaCompleta == null);
                e.consume();
                return;
            }
            if (e.isShortcutDown() && e.getCode() == KeyCode.Z) {
                JE_JE_SR_JA_ME_deshacer();
                e.consume();
                return;
            }
            if (e.getCode() == KeyCode.ESCAPE) {
                botonSeleccionar.setSelected(true);
                e.consume();
                return;
            }
            if (e.getCode().isDigitKey() && sesion.JE_JE_SR_JA_ME_puedeRegistrarClinica()) {
                int d = Integer.parseInt(e.getText().isBlank() ? "-1" : e.getText());
                List<ToggleButton> orden = new ArrayList<>(botonesHerramienta.values());
                // Orden de teclas: 1..n segÃºn la paleta; 0 = Sano.
                ToggleButton b = d == 0 ? botonesHerramienta.get(JE_JE_SR_JA_ME_EstadoDental.SANO)
                        : d - 1 < orden.size() && orden.get(d - 1).getUserData() != JE_JE_SR_JA_ME_EstadoDental.SANO ? orden.get(d - 1) : null;
                if (b != null) {
                    b.setSelected(true);
                    e.consume();
                }
            }
        };
        raiz.addEventFilter(KeyEvent.KEY_PRESSED, atajos);
    }

    // -------------------------------------------------------- pantalla completa

    /**
     * Lleva el escenario 3D (con su barra y la lÃ­nea de tiempo) a una ventana a pantalla
     * completa, ideal para explicarle el diagnÃ³stico al paciente. Al salir (Esc, F11 o el
     * botÃ³n) vuelve exactamente a su lugar en la ficha.
     */
    private void JE_JE_SR_JA_ME_pantallaCompleta(boolean activar) {
        if (!activar) {
            if (ventanaCompleta != null) {
                ventanaCompleta.close();
            }
            return;
        }
        if (ventanaCompleta != null) {
            return;
        }
        javafx.scene.layout.Pane padre = (javafx.scene.layout.Pane) escenario.getParent();
        int posicion = padre.getChildren().indexOf(escenario);
        Node clip = escenario.getClip();
        padre.getChildren().remove(escenario);
        escenario.setClip(null);

        javafx.scene.Scene principal = JE_JE_SR_JA_ME_JavaFxApplication.JE_JE_SR_JA_ME_get().JE_JE_SR_JA_ME_ventana().getScene();
        StackPane contenedor = new StackPane(escenario);
        contenedor.getStyleClass().setAll(principal.getRoot().getStyleClass());
        javafx.scene.Scene escena = new javafx.scene.Scene(contenedor);
        escena.getStylesheets().setAll(principal.getStylesheets());
        if (atajos != null) {
            escena.addEventFilter(KeyEvent.KEY_PRESSED, atajos);
        }

        ventanaCompleta = new javafx.stage.Stage();
        ventanaCompleta.initOwner(JE_JE_SR_JA_ME_JavaFxApplication.JE_JE_SR_JA_ME_get().JE_JE_SR_JA_ME_ventana());
        ventanaCompleta.setTitle(paciente.JE_JE_SR_JA_ME_nombreCompleto() + " Â· Odontograma 3D");
        ventanaCompleta.getIcons().setAll(JE_JE_SR_JA_ME_JavaFxApplication.JE_JE_SR_JA_ME_get().JE_JE_SR_JA_ME_ventana().getIcons());
        ventanaCompleta.setScene(escena);
        ventanaCompleta.setFullScreenExitHint("Pulsa Esc para salir de la pantalla completa");
        ventanaCompleta.setFullScreen(true);
        ventanaCompleta.fullScreenProperty().addListener((o, a, completa) -> {
            if (!completa && ventanaCompleta != null) {
                ventanaCompleta.close();
            }
        });
        ventanaCompleta.setOnHidden(e -> {
            contenedor.getChildren().remove(escenario);
            escenario.setClip(clip);
            padre.getChildren().add(posicion, escenario);
            ventanaCompleta = null;
            botonPantallaCompleta.setSelected(false);
            botonPantallaCompleta.setGraphic(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.MAXIMIZE, 15));
        });
        botonPantallaCompleta.setSelected(true);
        botonPantallaCompleta.setGraphic(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.MINIMIZE, 15));
        ventanaCompleta.show();
    }

    // ------------------------------------------------------------- refrescos

    private void JE_JE_SR_JA_ME_recargarTodo() {
        cronologia = clinica.JE_JE_SR_JA_ME_consultasCronologicas(paciente);
        JE_JE_SR_JA_ME_configurarLinea();
        fechaVista = null;
        JE_JE_SR_JA_ME_refrescarOdontograma();
    }

    private void JE_JE_SR_JA_ME_refrescarOdontograma() {
        actual = clinica.JE_JE_SR_JA_ME_odontograma(paciente, fechaVista);
        visor.JE_JE_SR_JA_ME_setOdontograma(actual);
        o2d.JE_JE_SR_JA_ME_setOdontograma(actual);
        JE_JE_SR_JA_ME_refrescarPanelPieza();
    }

    // --------------------------------------------------------- panel de pieza

    private void JE_JE_SR_JA_ME_refrescarPanelPieza() {
        panelPieza.getChildren().clear();
        if (piezaSel == null) {
            panelPieza.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_vacio(Feather.MOUSE_POINTER, "Selecciona una pieza",
                    "Haz clic sobre un diente en el modelo 3D o en el odontograma."));
            return;
        }
        int fdi = piezaSel;
        JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_EstadoPieza e = actual.JE_JE_SR_JA_ME_pieza(fdi);

        Label numero = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(String.valueOf(fdi), "pieza-numero");
        numero.setMinWidth(Region.USE_PREF_SIZE);
        Label nombre = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_nombre(fdi), "pieza-nombre");
        Label estado = e.JE_JE_SR_JA_ME_sana() ? JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Sana", "exito", Feather.CHECK)
                : e.JE_JE_SR_JA_ME_ausente() ? JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Ausente", "neutro", Feather.MINUS_CIRCLE)
                : JE_JE_SR_JA_ME_tienePatologia(e) ? JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Requiere tratamiento", "peligro", Feather.ALERT_CIRCLE)
                : JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Tratada", "info", Feather.SHIELD);
        VBox titulo = new VBox(2, nombre, estado);
        HBox cab = new HBox(12, numero, titulo);
        cab.setAlignment(Pos.CENTER_LEFT);

        // Diagrama grande de las cinco caras (clicable).
        javafx.scene.Group diagrama = JE_JE_SR_JA_ME_Odontograma2D.JE_JE_SR_JA_ME_diagramaPieza(fdi, e, 116, caraSel,
                (p, cara) -> JE_JE_SR_JA_ME_alPulsarPieza(p, cara), this::JE_JE_SR_JA_ME_volverSana);
        StackPane cajaDiagrama = new StackPane(diagrama);
        cajaDiagrama.setPadding(new Insets(14));
        cajaDiagrama.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 12;");

        VBox caras = new VBox(0);
        for (JE_JE_SR_JA_ME_Superficie s : JE_JE_SR_JA_ME_Superficie.CARAS) {
            JE_JE_SR_JA_ME_EstadoDental v = e.JE_JE_SR_JA_ME_visible(s);
            Label l = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(s.JE_JE_SR_JA_ME_etiqueta(fdi), s == caraSel ? "texto-fuerte" : "");
            Label est = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(e.JE_JE_SR_JA_ME_ausente() ? "â€”" : v.JE_JE_SR_JA_ME_getEtiqueta(), "texto-tenue");
            HBox fila = new HBox(8, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_muestra(e.JE_JE_SR_JA_ME_ausente() ? "#e2e8f0" : v.JE_JE_SR_JA_ME_getColor()), l, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_espaciador(), est);
            fila.getStyleClass().add("fila-cara");
            caras.getChildren().add(fila);
        }
        if (e.JE_JE_SR_JA_ME_general() != null) {
            HBox fila = new HBox(8, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_muestra(e.JE_JE_SR_JA_ME_general().JE_JE_SR_JA_ME_getColor()), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Pieza completa", "texto-fuerte"),
                    JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_espaciador(), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(e.JE_JE_SR_JA_ME_general().JE_JE_SR_JA_ME_getEtiqueta(), "texto-tenue"));
            fila.getStyleClass().add("fila-cara");
            caras.getChildren().add(fila);
        }

        Button enfocar = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("Ver de cerca", Feather.CROSSHAIR);
        enfocar.setOnAction(ev -> visor.JE_JE_SR_JA_ME_enfocar(fdi));
        Button tratar = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("Tratamiento", Feather.PLUS);
        tratar.setOnAction(ev -> JE_JE_SR_JA_ME_dialogoAgregarTratamiento(fdi, caraSel));
        tratar.setDisable(!sesion.JE_JE_SR_JA_ME_puedeRegistrarClinica());
        tratar.setVisible(!sesion.JE_JE_SR_JA_ME_esPaciente());
        tratar.setManaged(!sesion.JE_JE_SR_JA_ME_esPaciente());
        HBox acciones = new HBox(8, enfocar, tratar);
        enfocar.setMaxWidth(Double.MAX_VALUE);
        tratar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(enfocar, Priority.ALWAYS);
        HBox.setHgrow(tratar, Priority.ALWAYS);

        nota.setPromptText("Nota clÃ­nica para el prÃ³ximo registro (opcional)");
        nota.setPrefRowCount(2);
        nota.setWrapText(true);
        nota.setDisable(!sesion.JE_JE_SR_JA_ME_puedeRegistrarClinica() || fechaVista != null);
        nota.setVisible(!sesion.JE_JE_SR_JA_ME_esPaciente());
        nota.setManaged(!sesion.JE_JE_SR_JA_ME_esPaciente());

        VBox historial = new VBox(6);
        List<JE_JE_SR_JA_ME_Hallazgo> hs = clinica.JE_JE_SR_JA_ME_historialPieza(paciente, fdi);
        if (hs.isEmpty()) {
            historial.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Sin registros en esta pieza.", "texto-tenue"));
        }
        for (JE_JE_SR_JA_ME_Hallazgo h : hs.stream().limit(12).toList()) {
            boolean futuro = fechaVista != null && h.getFecha().isAfter(fechaVista);
            Label t = new Label(h.getEstado().JE_JE_SR_JA_ME_getEtiqueta()
                    + (h.getSuperficie().JE_JE_SR_JA_ME_esCara() ? " Â· " + h.getSuperficie().JE_JE_SR_JA_ME_etiqueta(fdi) : " Â· pieza"),
                    JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_muestra(h.getEstado().JE_JE_SR_JA_ME_getColor()));
            t.getStyleClass().add("texto-fuerte");
            t.setGraphicTextGap(8);
            Label d = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_fecha(h.getFecha()) + " Â· " + (h.getRegistradoPor() == null ? "" : h.getRegistradoPor()),
                    "texto-tenue", "texto-pequeno");
            VBox item = new VBox(2, t, d);
            if (h.getNota() != null) {
                Label n = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("â€œ" + h.getNota() + "â€", "texto-pequeno");
                n.setWrapText(true);
                item.getChildren().add(n);
            }
            item.getStyleClass().add("historial-item");
            if (futuro) {
                item.setOpacity(0.4);
            }
            historial.getChildren().add(item);
        }

        panelPieza.getChildren().addAll(cab, cajaDiagrama, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("CARAS", "campo-rotulo"), caras,
                acciones, nota, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("HISTORIA DE LA PIEZA", "campo-rotulo"), historial);
    }

    private static boolean JE_JE_SR_JA_ME_tienePatologia(JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_EstadoPieza e) {
        return (e.JE_JE_SR_JA_ME_general() != null && e.JE_JE_SR_JA_ME_general().JE_JE_SR_JA_ME_esPatologia())
                || e.JE_JE_SR_JA_ME_caras().values().stream().anyMatch(JE_JE_SR_JA_ME_EstadoDental::JE_JE_SR_JA_ME_esPatologia);
    }

    // ===================================================================== plan

    private Node JE_JE_SR_JA_ME_construirPlan() {
        Label titulo = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Plan de tratamiento", "tarjeta-titulo");
        Label sub = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Genera el plan desde el odontograma, marca lo realizado y entrega el presupuesto.",
                "tarjeta-subtitulo");
        Button generar = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_primario("Generar desde odontograma", Feather.ZAP);
        generar.setOnAction(e -> notificador.JE_JE_SR_JA_ME_intentar(() -> {
            int n = planes.JE_JE_SR_JA_ME_generarDesdeOdontograma(paciente);
            if (n == 0) {
                notificador.JE_JE_SR_JA_ME_info("El plan ya cubre todas las patologÃ­as del odontograma.");
            } else {
                notificador.JE_JE_SR_JA_ME_exito(n + (n == 1 ? " tratamiento agregado" : " tratamientos agregados") + " al plan.");
            }
            JE_JE_SR_JA_ME_refrescarPlan();
        }));
        Button agregar = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("Agregar tratamiento", Feather.PLUS);
        agregar.setOnAction(e -> JE_JE_SR_JA_ME_dialogoAgregarTratamiento(piezaSel, caraSel));
        Button exportar = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("Presupuesto PDF", Feather.DOWNLOAD);
        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tooltip(exportar, "PDF con los tratamientos, importes, lo pagado y el saldo");
        exportar.setOnAction(e -> JE_JE_SR_JA_ME_exportarPresupuesto());
        boolean puede = sesion.JE_JE_SR_JA_ME_puedeRegistrarClinica();
        generar.setDisable(!puede);
        agregar.setDisable(!puede);
        HBox botonesPlan = new HBox(8, generar, agregar, exportar);
        botonesPlan.getChildren().forEach(n -> ((Region) n).setMinWidth(Region.USE_PREF_SIZE));
        VBox cab = new VBox(10, new VBox(2, titulo, sub), botonesPlan);
        cab.setAlignment(Pos.CENTER_LEFT);

        JE_JE_SR_JA_ME_configurarTablaPlan();
        VBox.setVgrow(tablaPlan, Priority.ALWAYS);
        VBox tarjeta = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tarjeta(cab, totalesPlan, tablaPlan);
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        ScrollPane lateral = new ScrollPane(panelCuenta);
        lateral.setFitToWidth(true);
        lateral.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        lateral.setMinWidth(340);
        lateral.setPrefWidth(340);
        HBox v = new HBox(16, tarjeta, lateral);
        v.setPadding(new Insets(16));
        return v;
    }

    private void JE_JE_SR_JA_ME_configurarTablaPlan() {
        tablaPlan.getStyleClass().addAll("tabla");
        tablaPlan.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaPlan.setPlaceholder(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_vacio(Feather.CLIPBOARD, "Plan vacÃ­o",
                "Registra hallazgos en el odontograma y pulsa \"Generar desde odontograma\"."));

        TableColumn<JE_JE_SR_JA_ME_ItemPlan, JE_JE_SR_JA_ME_ItemPlan> pieza = JE_JE_SR_JA_ME_col("Pieza", i -> i);
        pieza.setPrefWidth(170);
        pieza.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(JE_JE_SR_JA_ME_ItemPlan i, boolean vacio) {
                super.updateItem(i, vacio);
                if (vacio || i == null) {
                    setGraphic(null);
                    return;
                }
                Label n = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(i.JE_JE_SR_JA_ME_ubicacion(), "texto-fuerte");
                Label d = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(i.JE_JE_SR_JA_ME_descripcionPieza(), "texto-tenue", "texto-pequeno");
                setGraphic(new VBox(1, n, d));
            }
        });
        TableColumn<JE_JE_SR_JA_ME_ItemPlan, JE_JE_SR_JA_ME_ItemPlan> trat = JE_JE_SR_JA_ME_col("Tratamiento", i -> i);
        trat.setPrefWidth(260);
        trat.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(JE_JE_SR_JA_ME_ItemPlan i, boolean vacio) {
                super.updateItem(i, vacio);
                if (vacio || i == null) {
                    setGraphic(null);
                    return;
                }
                Label n = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(i.getTratamiento().getNombre(), "texto-fuerte");
                VBox v = new VBox(1, n);
                if (i.getNota() != null) {
                    v.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(i.getNota(), "texto-tenue", "texto-pequeno"));
                }
                setGraphic(v);
            }
        });
        TableColumn<JE_JE_SR_JA_ME_ItemPlan, String> fecha = JE_JE_SR_JA_ME_col("Fecha", i -> i.getEstado() == JE_JE_SR_JA_ME_EstadoItem.REALIZADO
                ? "Realizado " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_fecha(i.getRealizado()) : "Plan " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_fecha(i.getCreado()));
        fecha.setPrefWidth(150);
        TableColumn<JE_JE_SR_JA_ME_ItemPlan, JE_JE_SR_JA_ME_ItemPlan> estado = JE_JE_SR_JA_ME_col("Estado", i -> i);
        estado.setPrefWidth(124);
        estado.setMinWidth(124);
        estado.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(JE_JE_SR_JA_ME_ItemPlan i, boolean vacio) {
                super.updateItem(i, vacio);
                if (vacio || i == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(switch (i.getEstado()) {
                    case PENDIENTE -> JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip(JE_JE_SR_JA_ME_EstadoItem.PENDIENTE.JE_JE_SR_JA_ME_getEtiqueta(), "alerta", Feather.CLOCK);
                    case REALIZADO -> JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Realizado", "exito", Feather.CHECK);
                    case CANCELADO -> JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Cancelado", "neutro", Feather.X);
                });
            }
        });
        TableColumn<JE_JE_SR_JA_ME_ItemPlan, String> precio = JE_JE_SR_JA_ME_col("Importe", i -> JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(i.getPrecio()));
        precio.setPrefWidth(110);
        precio.setStyle("-fx-alignment: CENTER-RIGHT;");
        TableColumn<JE_JE_SR_JA_ME_ItemPlan, JE_JE_SR_JA_ME_ItemPlan> acciones = JE_JE_SR_JA_ME_col("", i -> i);
        acciones.setPrefWidth(190);
        acciones.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(JE_JE_SR_JA_ME_ItemPlan i, boolean vacio) {
                super.updateItem(i, vacio);
                if (vacio || i == null) {
                    setGraphic(null);
                    return;
                }
                HBox h = new HBox(6);
                h.setAlignment(Pos.CENTER_RIGHT);
                boolean puede = sesion.JE_JE_SR_JA_ME_puedeRegistrarClinica();
                if (i.getEstado() == JE_JE_SR_JA_ME_EstadoItem.PENDIENTE) {
                    Button hecho = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.CHECK_CIRCLE, "Marcar como realizado");
                    hecho.setDisable(!puede);
                    hecho.setOnAction(e -> JE_JE_SR_JA_ME_realizar(i));
                    Button cancelar = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.X, "Cancelar tratamiento");
                    cancelar.setDisable(!puede);
                    cancelar.setOnAction(e -> notificador.JE_JE_SR_JA_ME_intentar(() -> {
                        planes.JE_JE_SR_JA_ME_cancelar(i);
                        JE_JE_SR_JA_ME_refrescarPlan();
                    }));
                    Button agendar = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.CALENDAR,
                            sesion.JE_JE_SR_JA_ME_esPaciente() ? "Pedir cita para este tratamiento" : "Agendar cita para este tratamiento");
                    agendar.setOnAction(e -> dialogoCita.JE_JE_SR_JA_ME_abrir(new JE_JE_SR_JA_ME_DialogoCita.JE_JE_SR_JA_ME_Sugerencia(paciente, null, null, i),
                            c -> JE_JE_SR_JA_ME_construirCabecera()));
                    if (sesion.JE_JE_SR_JA_ME_esPaciente()) {
                        h.getChildren().add(agendar);
                    } else {
                        h.getChildren().addAll(agendar, hecho, cancelar);
                    }
                } else if (i.getEstado() == JE_JE_SR_JA_ME_EstadoItem.CANCELADO) {
                    Button reabrir = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.ROTATE_CCW, "Volver a pendiente");
                    reabrir.setDisable(!puede);
                    reabrir.setOnAction(e -> notificador.JE_JE_SR_JA_ME_intentar(() -> {
                        planes.JE_JE_SR_JA_ME_reabrir(i);
                        JE_JE_SR_JA_ME_refrescarPlan();
                    }));
                    h.getChildren().add(reabrir);
                }
                if (i.getPieza() != null) {
                    Button ver = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.EYE, "Ver la pieza en el odontograma");
                    ver.setOnAction(e -> {
                        tabOdontograma.fire();
                        JE_JE_SR_JA_ME_seleccionar(i.getPieza(), i.getSuperficie());
                        visor.JE_JE_SR_JA_ME_enfocar(i.getPieza());
                    });
                    h.getChildren().add(ver);
                }
                setGraphic(h);
            }
        });
        tablaPlan.getColumns().setAll(List.of(pieza, trat, fecha, estado, precio, acciones));
    }

    private void JE_JE_SR_JA_ME_realizar(JE_JE_SR_JA_ME_ItemPlan i) {
        notificador.JE_JE_SR_JA_ME_confirmar("Marcar como realizado",
                i.getTratamiento().getNombre() + " en " + i.JE_JE_SR_JA_ME_ubicacion() + ".\n"
                        + (i.getTratamiento().getEstadoResultante() != null && i.getPieza() != null
                        ? "El odontograma se actualizarÃ¡: la pieza quedarÃ¡ como \""
                        + i.getTratamiento().getEstadoResultante().JE_JE_SR_JA_ME_getEtiqueta().toLowerCase() + "\"." : ""),
                "Confirmar", false, () -> notificador.JE_JE_SR_JA_ME_intentar(() -> {
                    planes.JE_JE_SR_JA_ME_realizar(i);
                    notificador.JE_JE_SR_JA_ME_exito(i.getTratamiento().getNombre() + " registrado como realizado.");
                    cronologia = clinica.JE_JE_SR_JA_ME_consultasCronologicas(paciente);
                    JE_JE_SR_JA_ME_configurarLinea();
                    fechaVista = null;
                    JE_JE_SR_JA_ME_refrescarOdontograma();
                    JE_JE_SR_JA_ME_refrescarPlan();
                }));
    }

    // ================================================================= pagos

    /** Estado de cuenta, pagos y botÃ³n para cobrar. */
    private void JE_JE_SR_JA_ME_refrescarCuenta() {
        panelCuenta.getChildren().clear();
        JE_JE_SR_JA_ME_PagoService.JE_JE_SR_JA_ME_Cuenta c = pagos.JE_JE_SR_JA_ME_cuenta(paciente);
        // Un solo nÃºmero protagonista: lo que debe hoy, o lo que falta pagar del plan.
        boolean debe = c.JE_JE_SR_JA_ME_deuda().signum() > 0;
        boolean cancelado = c.JE_JE_SR_JA_ME_porPagarDelPlan().signum() == 0 && c.totalPlan().signum() > 0;
        String rotulo = debe ? "DEBE (TRATAMIENTOS YA REALIZADOS)" : cancelado ? "PLAN PAGADO POR COMPLETO" : "POR PAGAR DEL PLAN";
        Label saldo = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(debe ? c.JE_JE_SR_JA_ME_deuda() : cancelado ? c.pagado() : c.JE_JE_SR_JA_ME_porPagarDelPlan()), "total-valor");
        saldo.setStyle(debe ? "-fx-text-fill: #dc2626;" : cancelado ? "-fx-text-fill: #16a34a;" : "");
        String detalle = "Pagado " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(c.pagado()) + " de " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(c.totalPlan())
                + (debe || cancelado ? "" : c.JE_JE_SR_JA_ME_aFavor().signum() > 0 ? " Â· incluye adelanto" : " Â· al dÃ­a");
        VBox caja = new VBox(2, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(rotulo, "campo-rotulo"), saldo,
                JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(detalle, "texto-tenue", "texto-pequeno"));
        caja.getStyleClass().add("total-caja");
        ProgressBar barra = new ProgressBar(c.JE_JE_SR_JA_ME_avance());
        barra.getStyleClass().add("avance");
        barra.setMaxWidth(Double.MAX_VALUE);

        VBox tarjeta = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tarjeta(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_cabecera("Pagos", Math.round(c.JE_JE_SR_JA_ME_avance() * 100) + " % del plan pagado"), caja, barra);
        if (sesion.JE_JE_SR_JA_ME_esPersonal()) {
            Button cobrar = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_primario("Registrar pago", Feather.DOLLAR_SIGN);
            cobrar.setMaxWidth(Double.MAX_VALUE);
            cobrar.setDisable(c.JE_JE_SR_JA_ME_porPagarDelPlan().signum() == 0);
            cobrar.setOnAction(e -> JE_JE_SR_JA_ME_dialogoPago(c));
            tarjeta.getChildren().add(cobrar);
        }
        VBox lista = new VBox(6);
        for (JE_JE_SR_JA_ME_Pago pg : pagos.JE_JE_SR_JA_ME_pagos(paciente)) {
            Label m = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(pg.getMonto()), "texto-fuerte");
            if (pg.isAnulado()) {
                m.setStyle("-fx-strikethrough: true;");
            }
            Label d = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(pg.getRecibo() + " Â· " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_fecha(pg.getFecha()) + " Â· " + pg.getMedio().JE_JE_SR_JA_ME_getEtiqueta(),
                    "texto-tenue", "texto-pequeno");
            Button pdfRecibo = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.PRINTER, "Recibo en PDF");
            pdfRecibo.setOnAction(e -> notificador.JE_JE_SR_JA_ME_intentar(() -> JE_JE_SR_JA_ME_abrirArchivo(pdf.JE_JE_SR_JA_ME_recibo(pg, pagos.JE_JE_SR_JA_ME_cuenta(paciente)))));
            HBox fila = new HBox(8, new VBox(1, m, d), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_espaciador());
            if (pg.isAnulado()) {
                fila.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Anulado", "neutro", null));
            } else if (sesion.JE_JE_SR_JA_ME_esPersonal()) {
                Button anular = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.SLASH, "Anular recibo");
                anular.setOnAction(e -> JE_JE_SR_JA_ME_dialogoAnular(pg));
                fila.getChildren().add(anular);
            }
            fila.getChildren().add(pdfRecibo);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("historial-item");
            lista.getChildren().add(fila);
        }
        if (lista.getChildren().isEmpty()) {
            lista.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("AÃºn no hay pagos registrados.", "texto-tenue"));
        }
        panelCuenta.getChildren().addAll(tarjeta, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tarjeta(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_cabecera("Historial de pagos", null), lista));
    }

    private void JE_JE_SR_JA_ME_dialogoPago(JE_JE_SR_JA_ME_PagoService.JE_JE_SR_JA_ME_Cuenta c) {
        BigDecimal sugerido = c.JE_JE_SR_JA_ME_deuda().signum() > 0 ? c.JE_JE_SR_JA_ME_deuda() : c.JE_JE_SR_JA_ME_porPagarDelPlan();
        javafx.scene.control.TextField monto = new javafx.scene.control.TextField(sugerido.toPlainString());
        com.dentavista.ui.JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_dinero(monto);
        ToggleGroup g = new ToggleGroup();
        javafx.scene.layout.FlowPane medios = new javafx.scene.layout.FlowPane(6, 6);
        for (JE_JE_SR_JA_ME_MedioPago m : JE_JE_SR_JA_ME_MedioPago.values()) {
            ToggleButton b = new ToggleButton(m.JE_JE_SR_JA_ME_getEtiqueta());
            b.setUserData(m);
            b.setToggleGroup(g);
            b.getStyleClass().add("hora-libre");
            if (m == JE_JE_SR_JA_ME_MedioPago.EFECTIVO) {
                b.setSelected(true);
            }
            medios.getChildren().add(b);
        }
        javafx.scene.control.TextField nota = new javafx.scene.control.TextField();
        nota.setPromptText("Ej. N.Âº de operaciÃ³n Yape, primera cuotaâ€¦");
        Label info = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Debe por tratamientos ya realizados: " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(c.JE_JE_SR_JA_ME_deuda())
                + " Â· Falta pagar del plan completo: " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(c.JE_JE_SR_JA_ME_porPagarDelPlan()), "texto-tenue");
        info.setWrapText(true);
        VBox cont = new VBox(12, info, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Monto (S/)", monto), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Medio de pago", medios), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Nota", nota));
        notificador.JE_JE_SR_JA_ME_dialogo(Feather.DOLLAR_SIGN, "Registrar pago", cont, "Registrar y emitir recibo", false, () -> {
            BigDecimal valor;
            try {
                valor = new BigDecimal(monto.getText().trim().replace(",", "."));
            } catch (NumberFormatException ex) {
                throw new JE_JE_SR_JA_ME_NegocioException("El monto debe ser un nÃºmero, por ejemplo 150.00");
            }
            JE_JE_SR_JA_ME_Pago pg = pagos.JE_JE_SR_JA_ME_registrar(paciente, valor, g.getSelectedToggle() == null ? null
                    : (JE_JE_SR_JA_ME_MedioPago) g.getSelectedToggle().getUserData(), nota.getText());
            notificador.JE_JE_SR_JA_ME_exito("Pago registrado: recibo " + pg.getRecibo() + " por " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(pg.getMonto()) + ".");
            JE_JE_SR_JA_ME_refrescarPlan();
            JE_JE_SR_JA_ME_abrirArchivo(pdf.JE_JE_SR_JA_ME_recibo(pg, pagos.JE_JE_SR_JA_ME_cuenta(paciente)));
        }, 480);
    }

    private void JE_JE_SR_JA_ME_dialogoAnular(JE_JE_SR_JA_ME_Pago pg) {
        javafx.scene.control.TextField motivo = new javafx.scene.control.TextField();
        motivo.setPromptText("Ej. Monto mal digitado");
        VBox cont = new VBox(10, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("El recibo " + pg.getRecibo() + " por " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(pg.getMonto())
                + " dejarÃ¡ de contar como pagado. Queda en el historial como anulado.", "texto-tenue"), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Motivo", motivo));
        notificador.JE_JE_SR_JA_ME_dialogo(Feather.SLASH, "Anular recibo", cont, "Anular", true, () -> {
            pagos.JE_JE_SR_JA_ME_anular(pg, motivo.getText());
            notificador.JE_JE_SR_JA_ME_exito("Recibo " + pg.getRecibo() + " anulado.");
            JE_JE_SR_JA_ME_refrescarPlan();
        }, 460);
    }

    private void JE_JE_SR_JA_ME_abrirArchivo(java.nio.file.Path archivo) {
        JE_JE_SR_JA_ME_JavaFxApplication.JE_JE_SR_JA_ME_get().JE_JE_SR_JA_ME_hostServices().showDocument(archivo.toUri().toString());
    }

    private void JE_JE_SR_JA_ME_refrescarPlan() {
        JE_JE_SR_JA_ME_refrescarCuenta();
        List<JE_JE_SR_JA_ME_ItemPlan> items = new ArrayList<>(planes.JE_JE_SR_JA_ME_items(paciente));
        // Primero lo pendiente (lo que hay que hacer y cobrar), luego lo realizado mÃ¡s reciente.
        items.sort(java.util.Comparator.comparingInt((JE_JE_SR_JA_ME_ItemPlan i) -> i.getEstado().ordinal())
                .thenComparing(i -> i.getEstado() == JE_JE_SR_JA_ME_EstadoItem.REALIZADO ? i.getRealizado() : i.getCreado(),
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        tablaPlan.getItems().setAll(items);
        JE_JE_SR_JA_ME_PlanService.JE_JE_SR_JA_ME_Totales t = planes.JE_JE_SR_JA_ME_totales(paciente);
        boolean pagado = pagos.JE_JE_SR_JA_ME_cuenta(paciente).JE_JE_SR_JA_ME_porPagarDelPlan().signum() == 0 && t.JE_JE_SR_JA_ME_total().signum() > 0;
        ProgressBar barra = new ProgressBar(t.JE_JE_SR_JA_ME_avance());
        barra.getStyleClass().add("avance");
        barra.setMaxWidth(Double.MAX_VALUE);
        barra.setPrefHeight(8);
        VBox avance = new VBox(6, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("AVANCE DEL PLAN Â· " + Math.round(t.JE_JE_SR_JA_ME_avance() * 100) + "%", "campo-rotulo"), barra);
        HBox.setHgrow(avance, Priority.ALWAYS);
        avance.setAlignment(Pos.CENTER_LEFT);
        HBox fila = new HBox(14,
                // Trabajo clÃ­nico (no dinero): lo cobrado y lo que falta pagar estÃ¡ en la tarjeta "Pagos".
                JE_JE_SR_JA_ME_caja("Por realizar", JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(t.pendiente()),
                        t.itemsPendientes() + " tratamientos" + (pagado && t.itemsPendientes() > 0 ? " Â· ya pagados" : ""), true),
                JE_JE_SR_JA_ME_caja("Realizado", JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(t.realizado()), t.itemsRealizados() + " tratamientos", false),
                JE_JE_SR_JA_ME_caja("Total del plan", JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(t.JE_JE_SR_JA_ME_total()), "", false),
                avance);
        fila.setAlignment(Pos.CENTER_LEFT);
        totalesPlan.getChildren().setAll(fila);
    }

    private Node JE_JE_SR_JA_ME_caja(String rotulo, String valor, String detalle, boolean acento) {
        Label v = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(valor, "total-valor");
        if (acento) {
            v.getStyleClass().add("acento");
        }
        VBox c = new VBox(2, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(rotulo.toUpperCase(), "campo-rotulo"), v,
                JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(detalle, "texto-tenue", "texto-pequeno"));
        c.getStyleClass().add("total-caja");
        c.setMinWidth(170);
        return c;
    }

    /** DiÃ¡logo para agregar un tratamiento del catÃ¡logo a una pieza / cara. */
    private void JE_JE_SR_JA_ME_dialogoAgregarTratamiento(Integer fdi, JE_JE_SR_JA_ME_Superficie cara) {
        ComboBox<JE_JE_SR_JA_ME_Tratamiento> trat = new ComboBox<>();
        trat.getItems().setAll(catalogo.JE_JE_SR_JA_ME_activos());
        trat.setMaxWidth(Double.MAX_VALUE);
        trat.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(JE_JE_SR_JA_ME_Tratamiento t, boolean vacio) {
                super.updateItem(t, vacio);
                setText(vacio || t == null ? null : t.getNombre() + "  â€”  " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(t.getPrecio()));
            }
        });
        trat.setButtonCell(trat.getCellFactory().call(null));

        ComboBox<Integer> pieza = new ComboBox<>();
        // Se escribe el nÃºmero ("16") o el nombre ("molar") y la lista se filtra.
        com.dentavista.ui.JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_buscable(pieza, JE_JE_SR_JA_ME_Pieza.TODAS, p -> p + " Â· " + JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_nombre(p));
        pieza.setValue(fdi);
        pieza.setMaxWidth(Double.MAX_VALUE);
        ComboBox<JE_JE_SR_JA_ME_Superficie> superficie = new ComboBox<>();
        superficie.getItems().setAll(JE_JE_SR_JA_ME_Superficie.CARAS);
        superficie.setValue(cara);
        superficie.setMaxWidth(Double.MAX_VALUE);
        superficie.setConverter(new StringConverter<>() {
            @Override
            public String toString(JE_JE_SR_JA_ME_Superficie s) {
                return s == null ? "" : s.JE_JE_SR_JA_ME_etiqueta(pieza.getValue() == null ? 11 : pieza.getValue());
            }

            @Override
            public JE_JE_SR_JA_ME_Superficie fromString(String s) {
                return null;
            }
        });
        Label ayuda = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("", "texto-tenue", "texto-pequeno");
        ayuda.setWrapText(true);
        Runnable actualizar = () -> {
            JE_JE_SR_JA_ME_Tratamiento t = trat.getValue();
            boolean requierePieza = t == null || t.isRequierePieza();
            boolean porCara = t != null && t.isPorCara();
            pieza.setDisable(!requierePieza);
            superficie.setDisable(!porCara);
            ayuda.setText(t == null ? "" : (t.getDescripcion() == null ? "" : t.getDescripcion())
                    + (porCara ? " Â· Se cobra por cara." : requierePieza ? " Â· Se aplica a la pieza completa." : " Â· Procedimiento general."));
        };
        trat.valueProperty().addListener((o, a, b) -> actualizar.run());
        actualizar.run();

        VBox contenido = new VBox(12, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Tratamiento", trat), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Pieza", pieza),
                JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Cara", superficie), ayuda);
        notificador.JE_JE_SR_JA_ME_dialogo(Feather.PLUS_CIRCLE, "Agregar tratamiento al plan", contenido, "Agregar", false, () -> {
            JE_JE_SR_JA_ME_ItemPlan i = planes.JE_JE_SR_JA_ME_agregar(paciente, trat.getValue(), pieza.getValue(), superficie.getValue());
            notificador.JE_JE_SR_JA_ME_exito(i.getTratamiento().getNombre() + " agregado al plan (" + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(i.getPrecio()) + ").");
            JE_JE_SR_JA_ME_refrescarPlan();
        }, 520);
    }

    // ================================================================ historial

    private Node JE_JE_SR_JA_ME_construirHistorial() {
        ScrollPane s = new ScrollPane(listaHistorial);
        s.setFitToWidth(true);
        listaHistorial.setPadding(new Insets(18, 24, 24, 24));
        return s;
    }

    private void JE_JE_SR_JA_ME_refrescarHistorial() {
        listaHistorial.getChildren().clear();
        List<JE_JE_SR_JA_ME_Consulta> consultas = clinica.JE_JE_SR_JA_ME_consultas(paciente);
        Label t = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Historia clÃ­nica", "titulo-pagina");
        Label s = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(consultas.size() + " consultas desde " + (consultas.isEmpty() ? "â€”"
                : JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_fecha(consultas.get(consultas.size() - 1).getFecha())), "subtitulo-pagina");
        VBox cab = new VBox(2, t, s);
        cab.setPadding(new Insets(0, 0, 14, 0));
        listaHistorial.getChildren().add(cab);
        if (consultas.isEmpty()) {
            listaHistorial.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_vacio(Feather.CLOCK, "Sin consultas",
                    "La primera consulta se abre automÃ¡ticamente al registrar un hallazgo."));
            return;
        }
        recetasDelPaciente = recetas.JE_JE_SR_JA_ME_recetas(paciente);
        List<JE_JE_SR_JA_ME_ItemPlan> realizados = planes.JE_JE_SR_JA_ME_items(paciente).stream()
                .filter(i -> i.getEstado() == JE_JE_SR_JA_ME_EstadoItem.REALIZADO && i.getConsulta() != null).toList();
        for (int k = 0; k < consultas.size(); k++) {
            JE_JE_SR_JA_ME_Consulta c = consultas.get(k);
            listaHistorial.getChildren().add(JE_JE_SR_JA_ME_hito(c, realizados, k == consultas.size() - 1));
        }
    }

    private Node JE_JE_SR_JA_ME_hito(JE_JE_SR_JA_ME_Consulta c, List<JE_JE_SR_JA_ME_ItemPlan> realizados, boolean ultimo) {
        Region punto = new Region();
        punto.getStyleClass().add("hito-punto");
        Region lin = new Region();
        lin.getStyleClass().add("hito-linea");
        VBox.setVgrow(lin, Priority.ALWAYS);
        lin.setVisible(!ultimo);
        VBox eje = new VBox(4, punto, lin);
        eje.setAlignment(Pos.TOP_CENTER);
        eje.setPadding(new Insets(20, 0, 0, 0));
        eje.setMinWidth(14);

        Label fecha = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_fechaLarga(c.getFecha()), "tarjeta-titulo");
        Label motivo = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(c.getMotivo() + (c.getOdontologo() != null ? "  Â·  " + c.getOdontologo().getNombre() : ""),
                "texto-tenue");
        Button ver = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("Ver odontograma en esta fecha", Feather.BOX);
        ver.setOnAction(e -> {
            tabOdontograma.fire();
            int idx = cronologia.indexOf(cronologia.stream().filter(x -> x.getId().equals(c.getId())).findFirst().orElse(null));
            if (idx >= 0) {
                linea.setValue(idx);
            }
        });
        HBox cab = new HBox(10, new VBox(2, fecha, motivo), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_espaciador(), ver);
        cab.setAlignment(Pos.CENTER_LEFT);

        FlowPane chips = new FlowPane(6, 6);
        for (JE_JE_SR_JA_ME_Hallazgo h : clinica.JE_JE_SR_JA_ME_hallazgosDe(c)) {
            String texto = h.getPieza() + (h.getSuperficie().JE_JE_SR_JA_ME_esCara() ? " " + h.getSuperficie().JE_JE_SR_JA_ME_sigla(h.getPieza()) : "")
                    + " Â· " + h.getEstado().JE_JE_SR_JA_ME_getEtiqueta();
            Label chip = new Label(texto, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_muestra(h.getEstado().JE_JE_SR_JA_ME_getColor()));
            chip.getStyleClass().addAll("chip", "neutro");
            chip.setGraphicTextGap(6);
            chips.getChildren().add(chip);
        }
        VBox tarjeta = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_tarjeta(cab);
        if (!chips.getChildren().isEmpty()) {
            tarjeta.getChildren().addAll(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("HALLAZGOS Y REGISTROS", "campo-rotulo"), chips);
        }
        List<com.dentavista.modelo.JE_JE_SR_JA_ME_Receta> recetasConsulta = recetasDelPaciente.stream()
                .filter(r -> r.getConsulta() != null && r.getConsulta().getId().equals(c.getId())).toList();
        if (!recetasConsulta.isEmpty()) {
            VBox rs = new VBox(4);
            for (com.dentavista.modelo.JE_JE_SR_JA_ME_Receta r : recetasConsulta) {
                String meds = r.getItems().stream().map(com.dentavista.modelo.JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item::getMedicamento)
                        .reduce((a, b) -> a + ", " + b).orElse("");
                Button imprimir = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.PRINTER, "Volver a imprimir la receta");
                imprimir.setOnAction(e -> notificador.JE_JE_SR_JA_ME_intentar(() -> JE_JE_SR_JA_ME_abrirArchivo(pdf.JE_JE_SR_JA_ME_receta(r))));
                Label l = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(meds, "");
                l.setWrapText(true);
                HBox f = new HBox(8, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.FILE_PLUS, 14), l, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_espaciador(), imprimir);
                f.setAlignment(Pos.CENTER_LEFT);
                rs.getChildren().add(f);
            }
            tarjeta.getChildren().addAll(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("RECETAS", "campo-rotulo"), rs);
        }
        List<JE_JE_SR_JA_ME_ItemPlan> deEsta = realizados.stream().filter(i -> i.getConsulta().getId().equals(c.getId())).toList();
        if (!deEsta.isEmpty()) {
            VBox trats = new VBox(4);
            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            for (JE_JE_SR_JA_ME_ItemPlan i : deEsta) {
                total = total.add(i.getPrecio());
                HBox f = new HBox(8, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_icono(Feather.CHECK_CIRCLE, 14),
                        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(i.getTratamiento().getNombre() + (i.getPieza() != null ? " Â· " + i.JE_JE_SR_JA_ME_ubicacion() : "")),
                        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_espaciador(), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(i.getPrecio()), "texto-fuerte"));
                f.setAlignment(Pos.CENTER_LEFT);
                trats.getChildren().add(f);
            }
            HBox tot = new HBox(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_espaciador(), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Total de la consulta: " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(total), "texto-fuerte"));
            tarjeta.getChildren().addAll(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("TRATAMIENTOS REALIZADOS", "campo-rotulo"), trats, tot);
        }
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        HBox fila = new HBox(14, eje, tarjeta);
        fila.setPadding(new Insets(0, 0, 14, 0));
        return fila;
    }

    // ================================================================= acceso

    /** Crea o renueva el usuario con el que el paciente entra a ver su odontograma y pedir citas. */
    private void JE_JE_SR_JA_ME_dialogoAcceso() {
        var actual = usuarios.JE_JE_SR_JA_ME_accesoDe(paciente);
        javafx.scene.control.TextField login = new javafx.scene.control.TextField(
                actual.map(com.dentavista.modelo.JE_JE_SR_JA_ME_Usuario::getUsuario).orElse(paciente.getDni()));
        javafx.scene.control.PasswordField clave = new javafx.scene.control.PasswordField();
        clave.setPromptText("MÃ­nimo 6 caracteres");
        Label info = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta(actual.isPresent()
                ? "Este paciente ya tiene acceso. Escribe una contraseÃ±a nueva para renovarla."
                : "Con este usuario el paciente podrÃ¡ ver su odontograma 3D, su presupuesto y pedir citas.",
                "texto-tenue");
        info.setWrapText(true);
        VBox c = new VBox(12, info, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Usuario", login), JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("ContraseÃ±a", clave));
        notificador.JE_JE_SR_JA_ME_dialogo(Feather.KEY, "Acceso del paciente", c, actual.isPresent() ? "Renovar contraseÃ±a" : "Crear acceso",
                false, () -> {
                    var u = usuarios.JE_JE_SR_JA_ME_crearAccesoPaciente(paciente, login.getText(), clave.getText());
                    notificador.JE_JE_SR_JA_ME_exito("Acceso listo: " + paciente.JE_JE_SR_JA_ME_nombreCompleto() + " entra con el usuario \"" + u.getUsuario() + "\".");
                }, 460);
    }

    // ===================================================================== PDF

    private void JE_JE_SR_JA_ME_exportarInforme() {
        notificador.JE_JE_SR_JA_ME_intentar(() -> {
            JE_JE_SR_JA_ME_Odontograma hoy = clinica.JE_JE_SR_JA_ME_odontograma(paciente);
            JE_JE_SR_JA_ME_PdfService.JE_JE_SR_JA_ME_Imagenes img = new JE_JE_SR_JA_ME_PdfService.JE_JE_SR_JA_ME_Imagenes(
                    JE_JE_SR_JA_ME_Imagenes.JE_JE_SR_JA_ME_aPng(JE_JE_SR_JA_ME_Capturas.JE_JE_SR_JA_ME_vista3D(hoy, JE_JE_SR_JA_ME_VisorDental3D.JE_JE_SR_JA_ME_Vista.FRONTAL, 1100, 700)),
                    JE_JE_SR_JA_ME_Imagenes.JE_JE_SR_JA_ME_aPng(JE_JE_SR_JA_ME_Capturas.JE_JE_SR_JA_ME_vista3D(hoy, JE_JE_SR_JA_ME_VisorDental3D.JE_JE_SR_JA_ME_Vista.SUPERIOR, 760, 520)),
                    JE_JE_SR_JA_ME_Imagenes.JE_JE_SR_JA_ME_aPng(JE_JE_SR_JA_ME_Capturas.JE_JE_SR_JA_ME_vista3D(hoy, JE_JE_SR_JA_ME_VisorDental3D.JE_JE_SR_JA_ME_Vista.INFERIOR, 760, 520)),
                    JE_JE_SR_JA_ME_Imagenes.JE_JE_SR_JA_ME_aPng(JE_JE_SR_JA_ME_Capturas.JE_JE_SR_JA_ME_vista2D(hoy, 1400, 400)));
            JE_JE_SR_JA_ME_entregar(pdf.JE_JE_SR_JA_ME_informe(paciente, hoy, clinica.JE_JE_SR_JA_ME_consultasCronologicas(paciente), img), "Informe clÃ­nico");
        });
    }

    private void JE_JE_SR_JA_ME_exportarPresupuesto() {
        notificador.JE_JE_SR_JA_ME_intentar(() -> JE_JE_SR_JA_ME_entregar(pdf.JE_JE_SR_JA_ME_presupuesto(paciente, planes.JE_JE_SR_JA_ME_items(paciente), planes.JE_JE_SR_JA_ME_totales(paciente),
                pagos.JE_JE_SR_JA_ME_cuenta(paciente)), "Presupuesto"));
    }

    private void JE_JE_SR_JA_ME_entregar(Path archivo, String que) {
        notificador.JE_JE_SR_JA_ME_exito(que + " guardado en " + archivo.getParent() + ".");
        JE_JE_SR_JA_ME_abrirArchivo(archivo);
    }

    private static <T> TableColumn<JE_JE_SR_JA_ME_ItemPlan, T> JE_JE_SR_JA_ME_col(String titulo, Function<JE_JE_SR_JA_ME_ItemPlan, T> valor) {
        TableColumn<JE_JE_SR_JA_ME_ItemPlan, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new SimpleObjectProperty<>(valor.apply(d.getValue())));
        c.setSortable(false);
        return c;
    }
}

