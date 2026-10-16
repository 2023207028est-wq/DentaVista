package com.dentavista;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Contexto de Spring (persistencia y servicios). La interfaz la arranca {@link Launcher}. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DentaVistaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DentaVistaApplication.class, args);
    }
}
