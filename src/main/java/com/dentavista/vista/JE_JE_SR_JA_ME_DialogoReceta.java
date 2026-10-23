package com.dentavista.vista;

import com.dentavista.JE_JE_SR_JA_ME_JavaFxApplication;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Medicamento;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Receta;
import com.dentavista.servicio.JE_JE_SR_JA_ME_NegocioException;
import com.dentavista.servicio.JE_JE_SR_JA_ME_PdfService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_RecetaService;
import com.dentavista.ui.JE_JE_SR_JA_ME_Notificador;
import com.dentavista.ui.JE_JE_SR_JA_ME_Ui;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo de receta: medicamentos del vademécum (o escritos a mano) con su indicación
 * habitual precargada. Cruza cada medicamento con las alergias y antecedentes del
 * paciente y exige confirmar las alertas antes de emitir.
 */
@Component
@RequiredArgsConstructor
public class JE_JE_SR_JA_ME_DialogoReceta {

    private final JE_JE_SR_JA_ME_RecetaService recetas;
    private final JE_JE_SR_JA_ME_PdfService pdf;
    private final JE_JE_SR_JA_ME_Notificador notificador;

    /** Una fila editable de la receta. */
    private record JE_JE_SR_JA_ME_Fila(ComboBox<String> medicamento, TextField indicacion, HBox nodo) {
    }

    public void JE_JE_SR_JA_ME_abrir(JE_JE_SR_JA_ME_Paciente p, Runnable alEmitir) {
        VBox filas = new VBox(8);
        List<JE_JE_SR_JA_ME_Fila> lista = new ArrayList<>();
        Label alertas = new Label();
        alertas.getStyleClass().add("dialogo-error");
        alertas.setWrapText(true);
        alertas.setMaxWidth(Double.MAX_VALUE);
        alertas.setVisible(false);
        alertas.setManaged(false);
        CheckBox confirmo = new CheckBox("Revisé las alertas y emito la receta bajo mi criterio clínico");
        confirmo.setWrapText(true);
        confirmo.setVisible(false);
        confirmo.setManaged(false);

        Runnable revisar = () -> {
            List<String> a = recetas.JE_JE_SR_JA_ME_alertas(p, JE_JE_SR_JA_ME_items(lista));
            alertas.setText(String.join("\n", a));
            boolean hay = !a.isEmpty();
            alertas.setVisible(hay);
            alertas.setManaged(hay);
            confirmo.setVisible(hay);
            confirmo.setManaged(hay);
            if (!hay) {
                confirmo.setSelected(false);
            }
        };

        Runnable agregar = () -> {
            ComboBox<String> med = new ComboBox<>();
            med.setEditable(true);
            for (JE_JE_SR_JA_ME_Medicamento m : JE_JE_SR_JA_ME_Medicamento.values()) {
                med.getItems().add(m.JE_JE_SR_JA_ME_getNombre());
            }
            med.setPromptText("Medicamento");
            med.setPrefWidth(250);
            TextField ind = new TextField();
            ind.setPromptText("Dosis, frecuencia y días");
            HBox.setHgrow(ind, Priority.ALWAYS);
            Button quitar = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_botonIcono(Feather.X, "Quitar");
            HBox nodo = new HBox(8, med, ind, quitar);
            nodo.setAlignment(Pos.CENTER_LEFT);
            JE_JE_SR_JA_ME_Fila f = new JE_JE_SR_JA_ME_Fila(med, ind, nodo);
            med.valueProperty().addListener((o, a, nombre) -> {
                JE_JE_SR_JA_ME_Medicamento.JE_JE_SR_JA_ME_porNombre(nombre).ifPresent(m -> {
                    if (ind.getText() == null || ind.getText().isBlank()) {
                        ind.setText(m.JE_JE_SR_JA_ME_getIndicacion());
                    }
                });
                revisar.run();
            });
            med.getEditor().focusedProperty().addListener((o, a, foco) -> {
                if (!foco) {
                    med.setValue(med.getEditor().getText());
                }
            });
            quitar.setOnAction(e -> {
                lista.remove(f);
                filas.getChildren().remove(nodo);
                revisar.run();
            });
            lista.add(f);
            filas.getChildren().add(nodo);
        };
        agregar.run();
        Button otra = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_secundario("Agregar medicamento", Feather.PLUS);
        otra.setOnAction(e -> agregar.run());

        TextArea indicaciones = new TextArea();
        indicaciones.setPromptText("Indicaciones generales: dieta blanda, no fumar, aplicar hielo las primeras 24 horas…");
        indicaciones.setPrefRowCount(2);
        indicaciones.setWrapText(true);

        VBox cont = new VBox(12);
        if (p.getAlergias() != null) {
            cont.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip("Alergia: " + p.getAlergias(), "peligro", Feather.ALERT_OCTAGON));
        }
        if (p.getAntecedentes() != null) {
            cont.getChildren().add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_chip(p.getAntecedentes(), "alerta", Feather.HEART));
        }
        cont.getChildren().addAll(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_etiqueta("Rp.", "tarjeta-titulo"), filas, otra, JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Indicaciones", indicaciones),
                alertas, confirmo);

        notificador.JE_JE_SR_JA_ME_dialogo(Feather.FILE_PLUS, "Receta para " + p.JE_JE_SR_JA_ME_nombreCompleto(), cont, "Emitir receta", false, () -> {
            lista.forEach(f -> {
                if (f.medicamento().getEditor() != null) {
                    f.medicamento().setValue(f.medicamento().getEditor().getText());
                }
            });
            revisar.run();
            if (alertas.isVisible() && !confirmo.isSelected()) {
                throw new JE_JE_SR_JA_ME_NegocioException("Hay alertas de alergia o contraindicación: revísalas y marca la confirmación, "
                        + "o cambia el medicamento.");
            }
            JE_JE_SR_JA_ME_Receta r = recetas.JE_JE_SR_JA_ME_emitir(p, JE_JE_SR_JA_ME_items(lista), indicaciones.getText(), confirmo.isSelected());
            notificador.JE_JE_SR_JA_ME_exito("Receta emitida con " + r.getItems().size() + (r.getItems().size() == 1 ? " medicamento." : " medicamentos."));
            alEmitir.run();
            JE_JE_SR_JA_ME_JavaFxApplication.JE_JE_SR_JA_ME_get().JE_JE_SR_JA_ME_hostServices().showDocument(pdf.JE_JE_SR_JA_ME_receta(r).toUri().toString());
        }, 680);
    }

    private static List<JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item> JE_JE_SR_JA_ME_items(List<JE_JE_SR_JA_ME_Fila> filas) {
        List<JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item> r = new ArrayList<>();
        for (JE_JE_SR_JA_ME_Fila f : filas) {
            String m = f.medicamento().getValue();
            if (m == null || m.isBlank()) {
                m = f.medicamento().getEditor() == null ? null : f.medicamento().getEditor().getText();
            }
            if (m != null && !m.isBlank()) {
                r.add(new JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item(m, f.indicacion().getText()));
            }
        }
        return r;
    }
}
