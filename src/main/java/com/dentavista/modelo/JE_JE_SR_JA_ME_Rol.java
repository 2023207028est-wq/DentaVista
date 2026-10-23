package com.dentavista.modelo;

/** Roles del consultorio y lo que cada uno puede hacer. */
public enum JE_JE_SR_JA_ME_Rol {

    ADMIN("Administrador"),
    ODONTOLOGO("Odontólogo"),
    ASISTENTE("Asistente"),
    /** Paciente con acceso propio: ve su odontograma y pide citas. */
    PACIENTE("Paciente");

    private final String etiqueta;

    JE_JE_SR_JA_ME_Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String JE_JE_SR_JA_ME_getEtiqueta() {
        return etiqueta;
    }

    /** Registra hallazgos en el odontograma y marca tratamientos como realizados. */
    public boolean JE_JE_SR_JA_ME_puedeRegistrarClinica() {
        return this == ADMIN || this == ODONTOLOGO;
    }

    /** Gestiona usuarios del sistema y el catálogo de precios. */
    public boolean JE_JE_SR_JA_ME_puedeAdministrar() {
        return this == ADMIN;
    }

    /** Personal del consultorio (todo menos paciente): ve la lista de pacientes y la agenda. */
    public boolean JE_JE_SR_JA_ME_esPersonal() {
        return this != PACIENTE;
    }
}
