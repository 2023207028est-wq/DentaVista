package com.dentavista.tresd;

import com.dentavista.dominio.JE_JE_SR_JA_ME_Pieza;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** El modelo OBJ se reconoce como una dentadura completa con sus caras clinicas. */
class JE_JE_SR_JA_ME_ModeloDentalTest {

    private static JE_JE_SR_JA_ME_ModeloDental modelo;

    @BeforeAll
    static void JE_JE_SR_JA_ME_cargar() {
        modelo = JE_JE_SR_JA_ME_ModeloDental.JE_JE_SR_JA_ME_get();
    }

    @Test
    void JE_JE_SR_JA_ME_reconoceLas32PiezasFdiYDosEncias() {
        assertThat(modelo.JE_JE_SR_JA_ME_dientes().keySet()).containsExactlyInAnyOrderElementsOf(JE_JE_SR_JA_ME_Pieza.TODAS);
        assertThat(modelo.JE_JE_SR_JA_ME_encias()).hasSize(2);
        assertThat(modelo.JE_JE_SR_JA_ME_encias()).extracting(JE_JE_SR_JA_ME_ModeloDental.JE_JE_SR_JA_ME_Encia::superior).containsExactlyInAnyOrder(true, false);
    }

    @Test
    void JE_JE_SR_JA_ME_lasPiezasSuperioresEstanPorEncimaDeLasInferiores() {
        for (int pos = 1; pos <= 8; pos++) {
            assertThat(modelo.JE_JE_SR_JA_ME_diente(10 + pos).centro()[1]).isGreaterThan(modelo.JE_JE_SR_JA_ME_diente(40 + pos).centro()[1]);
            assertThat(modelo.JE_JE_SR_JA_ME_diente(20 + pos).centro()[1]).isGreaterThan(modelo.JE_JE_SR_JA_ME_diente(30 + pos).centro()[1]);
        }
    }

    @Test
    void JE_JE_SR_JA_ME_elLadoDerechoDelPacienteQuedaEnXNegativo() {
        assertThat(modelo.JE_JE_SR_JA_ME_diente(16).centro()[0]).isNegative();
        assertThat(modelo.JE_JE_SR_JA_ME_diente(46).centro()[0]).isNegative();
        assertThat(modelo.JE_JE_SR_JA_ME_diente(26).centro()[0]).isPositive();
        assertThat(modelo.JE_JE_SR_JA_ME_diente(36).centro()[0]).isPositive();
    }

    @Test
    void JE_JE_SR_JA_ME_cadaDienteTieneSusCincoCaras() {
        for (JE_JE_SR_JA_ME_ModeloDental.JE_JE_SR_JA_ME_Diente d : modelo.JE_JE_SR_JA_ME_dientes().values()) {
            Map<JE_JE_SR_JA_ME_Superficie, Integer> conteo = new EnumMap<>(JE_JE_SR_JA_ME_Superficie.class);
            Arrays.stream(d.caraDeTriangulo()).forEach(s -> conteo.merge(s, 1, Integer::sum));
            assertThat(conteo.keySet())
                    .as("caras de la pieza %d", d.fdi())
                    .containsExactlyInAnyOrder(JE_JE_SR_JA_ME_Superficie.CARAS);
            conteo.forEach((cara, n) -> assertThat(n)
                    .as("triangulos de la cara %s de la pieza %d", cara, d.fdi())
                    // El borde incisal de incisivos y caninos es estrecho por anatomia.
                    .isGreaterThan(cara == JE_JE_SR_JA_ME_Superficie.OCLUSAL && JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_esAnterior(d.fdi()) ? 4 : 20));
        }
    }

    @Test
    void JE_JE_SR_JA_ME_laSubdivisionSuavizaLaMalla() {
        // Cada paso multiplica por ~4 las caras: el diente deja de verse facetado.
        assertThat(modelo.JE_JE_SR_JA_ME_diente(16).triangulos().size()).isGreaterThan(600);
    }
}
