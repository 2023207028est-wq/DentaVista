package com.dentavista.servicio;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Datos del consultorio que aparecen en la interfaz y en los PDF (application.properties). */
@ConfigurationProperties(prefix = "app.clinica")
public record DatosClinica(String nombre, String ruc, String direccion, String telefono, String email) {
}
