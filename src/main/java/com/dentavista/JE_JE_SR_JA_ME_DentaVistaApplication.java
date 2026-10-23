package com.dentavista;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Contexto de Spring (persistencia y servicios). La interfaz la arranca {@link JE_JE_SR_JA_ME_Launcher}. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class JE_JE_SR_JA_ME_DentaVistaApplication {

    public static void main(String[] args) {
        SpringApplication.run(JE_JE_SR_JA_ME_DentaVistaApplication.class, args);
    }
}
