package com.dentavista.servicio;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;

/** Valida entidades con Bean Validation y traduce el primer error a {@link JE_JE_SR_JA_ME_NegocioException}. */
@Component
@RequiredArgsConstructor
public class JE_JE_SR_JA_ME_Validacion {

    private final Validator validator;

    public <T> void JE_JE_SR_JA_ME_validar(T objeto) {
        Set<ConstraintViolation<T>> errores = validator.validate(objeto);
        errores.stream()
                .min(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .ifPresent(v -> {
                    throw new JE_JE_SR_JA_ME_NegocioException(v.getMessage());
                });
    }
}
