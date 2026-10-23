package com.dentavista.ui;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Las máscaras completan el formato y descartan lo inválido. */
class JE_JE_SR_JA_ME_MascarasTest {

    @Test
    void JE_JE_SR_JA_ME_laFechaSeArmaSoloConLosDigitos() {
        assertThat(JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_conBarras("12", false)).isEqualTo("12/");
        assertThat(JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_conBarras("1205", false)).isEqualTo("12/05/");
        assertThat(JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_conBarras("12051996", false)).isEqualTo("12/05/1996");
        assertThat(JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_conBarras("12", true)).isEqualTo("12");
    }

    @Test
    void JE_JE_SR_JA_ME_interpretaFechasCompletasYConAnioCorto() {
        assertThat(JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_interpretar("12/05/1996")).isEqualTo(LocalDate.of(1996, 5, 12));
        assertThat(JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_interpretar("12051996")).isEqualTo(LocalDate.of(1996, 5, 12));
        assertThat(JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_interpretar("120596")).isEqualTo(LocalDate.of(1996, 5, 12));
        int yy = LocalDate.now().getYear() % 100;
        assertThat(JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_interpretar("0101" + String.format("%02d", yy)).getYear()).isEqualTo(2000 + yy);
        assertThat(JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_interpretar("31/02/2020")).isNull();
        assertThat(JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_interpretar("12/05")).isNull();
    }

    @Test
    void JE_JE_SR_JA_ME_capitalizaNombresRespetandoConectores() {
        assertThat(JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_capitalizar("  valeria   QUISPE huanca ")).isEqualTo("Valeria Quispe Huanca");
        assertThat(JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_capitalizar("maría DE LA cruz")).isEqualTo("María de la Cruz");
        assertThat(JE_JE_SR_JA_ME_Mascaras.JE_JE_SR_JA_ME_capitalizar("dra. lucía paredes")).isEqualTo("Dra. Lucía Paredes");
    }
}
