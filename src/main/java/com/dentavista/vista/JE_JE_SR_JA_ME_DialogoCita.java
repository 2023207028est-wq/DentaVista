package com.dentavista.vista;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Cita;
import com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoItem;
import com.dentavista.modelo.JE_JE_SR_JA_ME_ItemPlan;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Usuario;
import com.dentavista.servicio.JE_JE_SR_JA_ME_CitaService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_NegocioException;
import com.dentavista.servicio.JE_JE_SR_JA_ME_PacienteService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_PlanService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_SesionService;
import com.dentavista.ui.JE_JE_SR_JA_ME_Formato;
import com.dentavista.ui.JE_JE_SR_JA_ME_Notificador;
import com.dentavista.ui.JE_JE_SR_JA_ME_Ui;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * DiÃ¡logo para agendar (personal) o solicitar (paciente) una cita. Muestra solo los
 * horarios libres del odontÃ³logo elegido y permite ligar la cita a un tratamiento
 * pendiente del plan.
 */
@Component
@RequiredArgsConstructor
public class JE_JE_SR_JA_ME_DialogoCita {

    /** Valores iniciales (cualquiera puede ser null). */
    public record JE_JE_SR_JA_ME_Sugerencia(JE_JE_SR_JA_ME_Paciente paciente, JE_JE_SR_JA_ME_Usuario odontologo, LocalDateTime inicio, JE_JE_SR_JA_ME_ItemPlan item) {
    }

    private final JE_JE_SR_JA_ME_CitaService citas;
    private final JE_JE_SR_JA_ME_PacienteService pacientes;
    private final JE_JE_SR_JA_ME_PlanService planes;
    private final JE_JE_SR_JA_ME_SesionService sesion;
    private final JE_JE_SR_JA_ME_Notificador notificador;

    public void JE_JE_SR_JA_ME_abrir(JE_JE_SR_JA_ME_Sugerencia s, Consumer<JE_JE_SR_JA_ME_Cita> alGuardar) {
        JE_JE_SR_JA_ME_mostrar(s, null, alGuardar);
    }

    /** Mismo formulario, con la cita actual precargada: se elige nuevo dÃ­a, hora u odontÃ³logo. */
    public void JE_JE_SR_JA_ME_reprogramar(JE_JE_SR_JA_ME_Cita cita, Consumer<JE_JE_SR_JA_ME_Cita> alGuardar) {
        JE_JE_SR_JA_ME_mostrar(new JE_JE_SR_JA_ME_Sugerencia(cita.getPaciente(), cita.getOdontologo(), cita.getInicio(), cita.getItemPlan()), cita, alGuardar);
    }

    private void JE_JE_SR_JA_ME_mostrar(JE_JE_SR_JA_ME_Sugerencia s, JE_JE_SR_JA_ME_Cita existente, Consumer<JE_JE_SR_JA_ME_Cita> alGuardar) {
        boolean esPaciente = sesion.JE_JE_SR_JA_ME_esPaciente();
        boolean reprogramando = existente != null;
        JE_JE_SR_JA_ME_Paciente fijo = esPaciente ? sesion.JE_JE_SR_JA_ME_pacientePropio() : s.paciente();

        ComboBox<JE_JE_SR_JA_ME_Paciente> paciente = new ComboBox<>();
        paciente.setMaxWidth(Double.MAX_VALUE);
        if (fijo != null) {
            paciente.getItems().setAll(fijo);
            paciente.setValue(fijo);
            paciente.setDisable(true);
        } else {
            // Se busca escribiendo parte del nombre o del DNI.
            com.dentavista.ui.JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_buscable(paciente, pacientes.JE_JE_SR_JA_ME_buscar(""), p -> p.JE_JE_SR_JA_ME_nombreCompleto() + " Â· DNI " + p.getDni());
        }
        if (fijo != null) {
            paciente.setConverter(JE_JE_SR_JA_ME_conv(p -> p.JE_JE_SR_JA_ME_nombreCompleto() + " Â· DNI " + p.getDni(), "Elige un paciente"));
        }
        paciente.setPromptText("Escribe nombre o DNI del paciente");

        ComboBox<JE_JE_SR_JA_ME_Usuario> odontologo = new ComboBox<>();
        odontologo.getItems().setAll(citas.JE_JE_SR_JA_ME_odontologos());
        odontologo.setMaxWidth(Double.MAX_VALUE);
        odontologo.setConverter(JE_JE_SR_JA_ME_conv(JE_JE_SR_JA_ME_Usuario::getNombre, "Elige un odontÃ³logo"));
        odontologo.setValue(s.odontologo() != null ? s.odontologo()
                : odontologo.getItems().isEmpty() ? null : odontologo.getItems().get(0));

        DatePicker fecha = new DatePicker(s.inicio() != null ? s.inicio().toLocalDate() : JE_JE_SR_JA_ME_siguienteDiaHabil());
        com.dentavista.ui.JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_fecha(fecha);
        fecha.setMaxWidth(Double.MAX_VALUE);
        fecha.setDayCellFactory(d -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean vacio) {
                super.updateItem(item, vacio);
                setDisable(vacio || item.isBefore(LocalDate.now()) || item.getDayOfWeek() == DayOfWeek.SUNDAY);
            }
        });

        ToggleGroup gDur = new ToggleGroup();
        HBox duraciones = new HBox(4);
        for (int m : new int[]{30, 60, 90}) {
            ToggleButton b = new ToggleButton(m + " min");
            b.setUserData(m);
            b.setToggleGroup(gDur);
            b.getStyleClass().add(m == 30 ? "left-pill" : m == 90 ? "right-pill" : "center-pill");
            if (m == (reprogramando ? existente.getDuracion() : 30)) {
                b.setSelected(true);
            }
            duraciones.getChildren().add(b);
        }

        ComboBox<JE_JE_SR_JA_ME_ItemPlan> tratamiento = new ComboBox<>();
        tratamiento.setMaxWidth(Double.MAX_VALUE);
        tratamiento.setPromptText("Consulta o control (sin tratamiento)");
        tratamiento.setConverter(JE_JE_SR_JA_ME_conv(i -> i.getTratamiento().getNombre() + (i.getPieza() != null ? " Â· " + i.JE_JE_SR_JA_ME_ubicacion() : "")
                + " Â· " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_soles(i.getPrecio()), "Consulta o control (sin tratamiento)"));
        Runnable cargarTratamientos = () -> {
            JE_JE_SR_JA_ME_Paciente p = paciente.getValue();
            tratamiento.getItems().clear();
            tratamiento.getItems().add(null);
            if (p != null) {
                planes.JE_JE_SR_JA_ME_items(p).stream().filter(i -> i.getEstado() == JE_JE_SR_JA_ME_EstadoItem.PENDIENTE).forEach(tratamiento.getItems()::add);
            }
            tratamiento.setValue(s.item());
        };
        cargarTratamientos.run();
        paciente.valueProperty().addListener((o, a, b) -> cargarTratamientos.run());

        TextField motivo = new TextField(reprogramando ? existente.getMotivo() : null);
        motivo.setPromptText(esPaciente ? "CuÃ©ntanos quÃ© necesitas (opcional)" : "Motivo (opcional)");

        FlowPane horas = new FlowPane(6, 6);
        ToggleGroup gHora = new ToggleGroup();
        Label sinHoras = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("", "texto-tenue", "texto-pequeno");
        Runnable cargarHoras = () -> {
            horas.getChildren().clear();
            JE_JE_SR_JA_ME_Usuario od = odontologo.getValue();
            LocalDate dia = fecha.getValue();
            int dur = gDur.getSelectedToggle() == null ? 30 : (int) gDur.getSelectedToggle().getUserData();
            if (od == null || dia == null) {
                sinHoras.setText("Elige odontÃ³logo y fecha.");
                return;
            }
            List<LocalTime> libres = citas.JE_JE_SR_JA_ME_horariosLibres(od, dia, dur, existente);
            sinHoras.setText(libres.isEmpty()
                    ? "No hay horarios libres ese dÃ­a. Prueba con otra fecha u odontÃ³logo."
                    : libres.size() + " horarios libres Â· " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_fechaLarga(dia.atStartOfDay()));
            for (LocalTime h : libres) {
                ToggleButton b = new ToggleButton(h.toString());
                b.setUserData(h);
                b.setToggleGroup(gHora);
                b.getStyleClass().add("hora-libre");
                if (s.inicio() != null && s.inicio().toLocalDate().equals(dia) && s.inicio().toLocalTime().equals(h)) {
                    b.setSelected(true);
                }
                horas.getChildren().add(b);
            }
        };
        cargarHoras.run();
        odontologo.valueProperty().addListener((o, a, b) -> cargarHoras.run());
        fecha.valueProperty().addListener((o, a, b) -> cargarHoras.run());
        gDur.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                a.setSelected(true);
                return;
            }
            cargarHoras.run();
        });

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(12);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Paciente", paciente), 0, 0, 2, 1);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("OdontÃ³logo", odontologo), 0, 1);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Fecha", fecha), 1, 1);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("DuraciÃ³n", duraciones), 0, 2, 2, 1);
        VBox bloqueHoras = new VBox(6, horas, sinHoras);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Horario libre", bloqueHoras), 0, 3, 2, 1);
        if (!reprogramando) {
            g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Tratamiento del plan", tratamiento), 0, 4, 2, 1);
            g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Motivo", motivo), 0, 5, 2, 1);
        }
        javafx.scene.layout.ColumnConstraints c = new javafx.scene.layout.ColumnConstraints();
        c.setPercentWidth(50);
        c.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c, c);
        VBox contenido = new VBox(10, g);
        if (reprogramando) {
            contenido.getChildren().add(0, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Cita actual: " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_fechaHora(existente.getInicio()) + " con "
                    + existente.getOdontologo().getNombre() + " Â· " + existente.JE_JE_SR_JA_ME_descripcion(), "info", Feather.CALENDAR));
        }
        if (esPaciente) {
            contenido.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Tu solicitud quedarÃ¡ \"por confirmar\" hasta que el consultorio la confirme.",
                    "info", Feather.INFO));
        }

        String titulo = reprogramando ? "Reprogramar cita" : esPaciente ? "Pedir una cita" : "Agendar cita";
        String accion = reprogramando ? "Reprogramar" : esPaciente ? "Solicitar cita" : "Agendar";
        notificador.JE_JE_SR_JA_ME_dialogo(Feather.CALENDAR, titulo, contenido, accion, false, () -> {
                    if (gHora.getSelectedToggle() == null) {
                        throw new JE_JE_SR_JA_ME_NegocioException("Elige uno de los horarios libres.");
                    }
                    LocalTime h = (LocalTime) gHora.getSelectedToggle().getUserData();
                    int dur = (int) gDur.getSelectedToggle().getUserData();
                    if (reprogramando) {
                        JE_JE_SR_JA_ME_Cita cita = citas.JE_JE_SR_JA_ME_reprogramar(existente, odontologo.getValue(), fecha.getValue().atTime(h), dur);
                        notificador.JE_JE_SR_JA_ME_exito("Cita reprogramada para el " + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_fechaHora(cita.getInicio())
                                + (esPaciente ? ". Queda por confirmar." : " con " + cita.getOdontologo().getNombre() + "."));
                        alGuardar.accept(cita);
                        return;
                    }
                    JE_JE_SR_JA_ME_Cita cita = citas.JE_JE_SR_JA_ME_agendar(paciente.getValue(), odontologo.getValue(), fecha.getValue().atTime(h), dur,
                            motivo.getText(), tratamiento.getValue());
                    notificador.JE_JE_SR_JA_ME_exito((esPaciente ? "Solicitud enviada: " : "Cita agendada: ")
                            + JE_JE_SR_JA_ME_Formato.JE_JE_SR_JA_ME_fechaHora(cita.getInicio()) + " con " + cita.getOdontologo().getNombre() + ".");
                    alGuardar.accept(cita);
                }, 600);
    }

    private static LocalDate JE_JE_SR_JA_ME_siguienteDiaHabil() {
        LocalDate d = LocalDate.now().plusDays(1);
        return d.getDayOfWeek() == DayOfWeek.SUNDAY ? d.plusDays(1) : d;
    }

    private static <T> StringConverter<T> JE_JE_SR_JA_ME_conv(java.util.function.Function<T, String> f, String vacio) {
        return new StringConverter<>() {
            @Override
            public String toString(T t) {
                return t == null ? vacio : f.apply(t);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }
}

