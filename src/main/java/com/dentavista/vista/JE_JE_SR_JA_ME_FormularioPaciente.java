package com.dentavista.vista;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.servicio.JE_JE_SR_JA_ME_PacienteService;
import com.dentavista.ui.JE_JE_SR_JA_ME_Notificador;
import com.dentavista.ui.JE_JE_SR_JA_ME_Ui;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.time.LocalDate;
import java.util.function.Consumer;

/** Diálogo de alta / edición de paciente (lo usan la lista y la ficha). */
final class JE_JE_SR_JA_ME_FormularioPaciente {

    private JE_JE_SR_JA_ME_FormularioPaciente() {
    }

    static void JE_JE_SR_JA_ME_abrir(JE_JE_SR_JA_ME_Notificador notificador, JE_JE_SR_JA_ME_PacienteService servicio, JE_JE_SR_JA_ME_Paciente existente, Consumer<JE_JE_SR_JA_ME_Paciente> alGuardar) {
        JE_JE_SR_JA_ME_Paciente p = existente != null ? existente : new JE_JE_SR_JA_ME_Paciente();

        TextField dni = new TextField(p.getDni());
        JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_soloDigitos(dni, 8);
        dni.setPromptText("8 dígitos");
        TextField nombres = new TextField(p.getNombres());
        TextField apellidos = new TextField(p.getApellidos());
        DatePicker nacimiento = new DatePicker(p.getFechaNacimiento());
        com.dentavista.ui.JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_fecha(nacimiento);
        com.dentavista.ui.JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_nombrePropio(nombres);
        com.dentavista.ui.JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_nombrePropio(apellidos);
        ComboBox<String> sexo = new ComboBox<>();
        sexo.getItems().addAll("Femenino", "Masculino", "Otro");
        sexo.setValue(p.getSexo() == null ? null : switch (p.getSexo()) {
            case "F" -> "Femenino";
            case "M" -> "Masculino";
            default -> "Otro";
        });
        TextField telefono = new TextField(p.getTelefono());
        TextField email = new TextField(p.getEmail());
        com.dentavista.ui.JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_celular(telefono);
        com.dentavista.ui.JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_correo(email);
        TextField direccion = new TextField(p.getDireccion());
        TextField alergias = new TextField(p.getAlergias());
        alergias.setPromptText("Penicilina, látex, anestésicos… (vacío si no tiene)");
        TextArea antecedentes = new TextArea(p.getAntecedentes());
        antecedentes.setPromptText("Diabetes, hipertensión, anticoagulantes, embarazo…");
        antecedentes.setPrefRowCount(2);
        antecedentes.setWrapText(true);

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(12);
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(50);
        c.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c, c);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("DNI *", dni), 0, 0);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Fecha de nacimiento", nacimiento), 1, 0);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Nombres *", nombres), 0, 1);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Apellidos *", apellidos), 1, 1);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Sexo", sexo), 0, 2);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Teléfono", telefono), 1, 2);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Correo", email), 0, 3);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Dirección", direccion), 1, 3);
        VBox alerta = JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Alergias", alergias);
        g.add(alerta, 0, 4, 2, 1);
        g.add(JE_JE_SR_JA_ME_Ui.JE_JE_SR_JA_ME_campo("Antecedentes médicos", antecedentes), 0, 5, 2, 1);

        notificador.JE_JE_SR_JA_ME_dialogo(existente == null ? Feather.USER_PLUS : Feather.EDIT_2,
                existente == null ? "Nuevo paciente" : "Editar paciente", g,
                existente == null ? "Registrar paciente" : "Guardar cambios", false, () -> {
                    p.setDni(dni.getText());
                    p.setNombres(nombres.getText());
                    p.setApellidos(apellidos.getText());
                    LocalDate f = nacimiento.getValue();
                    p.setFechaNacimiento(f);
                    p.setSexo(sexo.getValue() == null ? null : sexo.getValue().substring(0, 1));
                    p.setTelefono(JE_JE_SR_JA_ME_vacioANull(telefono.getText()));
                    p.setEmail(JE_JE_SR_JA_ME_vacioANull(email.getText()));
                    p.setDireccion(JE_JE_SR_JA_ME_vacioANull(direccion.getText()));
                    p.setAlergias(JE_JE_SR_JA_ME_vacioANull(alergias.getText()));
                    p.setAntecedentes(JE_JE_SR_JA_ME_vacioANull(antecedentes.getText()));
                    JE_JE_SR_JA_ME_Paciente guardado = servicio.JE_JE_SR_JA_ME_guardar(p);
                    notificador.JE_JE_SR_JA_ME_exito(existente == null
                            ? "Paciente " + guardado.JE_JE_SR_JA_ME_nombreCompleto() + " registrado."
                            : "Datos de " + guardado.JE_JE_SR_JA_ME_nombreCompleto() + " actualizados.");
                    alGuardar.accept(guardado);
                }, 620);
        dni.requestFocus();
    }

    private static String JE_JE_SR_JA_ME_vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
