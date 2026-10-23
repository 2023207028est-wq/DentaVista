package com.dentavista.dominio;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental.*;
import static com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas de reconstrucción del odontograma a partir de los registros clínicos. */
class JE_JE_SR_JA_ME_OdontogramaTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 1, 10, 9, 0);
    private final List<JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_Registro> registros = new ArrayList<>();
    private long secuencia;

    private void JE_JE_SR_JA_ME_registrar(int dias, int pieza, JE_JE_SR_JA_ME_Superficie s, JE_JE_SR_JA_ME_EstadoDental e) {
        registros.add(new JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_Registro(pieza, s, e, T0.plusDays(dias), secuencia++));
    }

    @Test
    void JE_JE_SR_JA_ME_sinRegistrosLas32PiezasEstanSanas() {
        JE_JE_SR_JA_ME_Odontograma o = JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_vacio();
        assertThat(o.JE_JE_SR_JA_ME_piezas()).hasSize(32).allMatch(JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_EstadoPieza::JE_JE_SR_JA_ME_sana);
        assertThat(o.JE_JE_SR_JA_ME_patologias()).isEmpty();
        assertThat(o.JE_JE_SR_JA_ME_piezasPresentes()).isEqualTo(32);
    }

    @Test
    void JE_JE_SR_JA_ME_unaCariesSoloAfectaASuCara() {
        JE_JE_SR_JA_ME_registrar(0, 16, OCLUSAL, CARIES);
        JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_EstadoPieza p = JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_reconstruir(registros).JE_JE_SR_JA_ME_pieza(16);
        assertThat(p.JE_JE_SR_JA_ME_visible(OCLUSAL)).isEqualTo(CARIES);
        assertThat(p.JE_JE_SR_JA_ME_visible(MESIAL)).isEqualTo(SANO);
        assertThat(p.JE_JE_SR_JA_ME_descripcion()).isEqualTo("Caries O");
    }

    @Test
    void JE_JE_SR_JA_ME_laObturacionPosteriorReemplazaALaCaries() {
        JE_JE_SR_JA_ME_registrar(0, 16, OCLUSAL, CARIES);
        JE_JE_SR_JA_ME_registrar(30, 16, OCLUSAL, OBTURADO);
        JE_JE_SR_JA_ME_Odontograma o = JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_reconstruir(registros);
        assertThat(o.JE_JE_SR_JA_ME_pieza(16).JE_JE_SR_JA_ME_cara(OCLUSAL)).isEqualTo(OBTURADO);
        assertThat(o.JE_JE_SR_JA_ME_patologias()).isEmpty();
    }

    @Test
    void JE_JE_SR_JA_ME_laLineaDeTiempoMuestraLaBocaEnUnaFechaPasada() {
        JE_JE_SR_JA_ME_registrar(0, 16, OCLUSAL, CARIES);
        JE_JE_SR_JA_ME_registrar(30, 16, OCLUSAL, OBTURADO);
        JE_JE_SR_JA_ME_Odontograma antes = JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_reconstruir(registros, T0.plusDays(10));
        assertThat(antes.JE_JE_SR_JA_ME_pieza(16).JE_JE_SR_JA_ME_cara(OCLUSAL)).isEqualTo(CARIES);
        assertThat(antes.JE_JE_SR_JA_ME_patologias()).extracting(JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_Patologia::pieza).containsExactly(16);
    }

    @Test
    void JE_JE_SR_JA_ME_ausenteBorraLasCarasYPintaTodaLaPieza() {
        JE_JE_SR_JA_ME_registrar(0, 36, OCLUSAL, CARIES);
        JE_JE_SR_JA_ME_registrar(1, 36, PIEZA, AUSENTE);
        JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_EstadoPieza p = JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_reconstruir(registros).JE_JE_SR_JA_ME_pieza(36);
        assertThat(p.JE_JE_SR_JA_ME_ausente()).isTrue();
        assertThat(p.JE_JE_SR_JA_ME_caras()).isEmpty();
        assertThat(p.JE_JE_SR_JA_ME_visible(OCLUSAL)).isEqualTo(AUSENTE);
    }

    @Test
    void JE_JE_SR_JA_ME_laEndodonciaConservaLasCarasPropias() {
        JE_JE_SR_JA_ME_registrar(0, 46, MESIAL, CARIES);
        JE_JE_SR_JA_ME_registrar(1, 46, PIEZA, ENDODONCIA);
        JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_EstadoPieza p = JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_reconstruir(registros).JE_JE_SR_JA_ME_pieza(46);
        assertThat(p.JE_JE_SR_JA_ME_visible(MESIAL)).isEqualTo(CARIES);
        assertThat(p.JE_JE_SR_JA_ME_visible(OCLUSAL)).isEqualTo(ENDODONCIA);
    }

    @Test
    void JE_JE_SR_JA_ME_sanoSobreLaPiezaLaRestableceCompleta() {
        JE_JE_SR_JA_ME_registrar(0, 11, VESTIBULAR, FRACTURA);
        JE_JE_SR_JA_ME_registrar(0, 11, PIEZA, CORONA);
        JE_JE_SR_JA_ME_registrar(1, 11, PIEZA, SANO);
        assertThat(JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_reconstruir(registros).JE_JE_SR_JA_ME_pieza(11).JE_JE_SR_JA_ME_sana()).isTrue();
    }

    @Test
    void JE_JE_SR_JA_ME_elOrdenEsCronologicoAunqueLosRegistrosLleguenDesordenados() {
        registros.add(new JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_Registro(21, OCLUSAL, OBTURADO, T0.plusDays(5), 1));
        registros.add(new JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_Registro(21, OCLUSAL, CARIES, T0, 2));
        assertThat(JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_reconstruir(registros).JE_JE_SR_JA_ME_pieza(21).JE_JE_SR_JA_ME_cara(OCLUSAL)).isEqualTo(OBTURADO);
    }

    @Test
    void JE_JE_SR_JA_ME_lasPatologiasIndicanElTratamientoSugerido() {
        JE_JE_SR_JA_ME_registrar(0, 16, OCLUSAL, CARIES);
        JE_JE_SR_JA_ME_registrar(0, 48, PIEZA, EXTRACCION_INDICADA);
        JE_JE_SR_JA_ME_registrar(0, 26, PIEZA, ENDODONCIA_INDICADA);
        assertThat(JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_reconstruir(registros).JE_JE_SR_JA_ME_patologias())
                .extracting(pat -> pat.estado().JE_JE_SR_JA_ME_getTratamientoSugerido())
                .containsExactlyInAnyOrder("OBT", "EXO", "END");
    }

    @Test
    void JE_JE_SR_JA_ME_rechazaPiezasQueNoExistenEnFdi() {
        assertThatThrownBy(() -> JE_JE_SR_JA_ME_registrar(0, 19, OCLUSAL, CARIES)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JE_JE_SR_JA_ME_registrar(0, 51, OCLUSAL, CARIES)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void JE_JE_SR_JA_ME_nombresClinicosSegunLaPieza() {
        assertThat(JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_nombre(16)).isEqualTo("Primer molar superior derecho");
        assertThat(JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_nombre(38)).isEqualTo("Tercer molar inferior izquierdo");
        assertThat(OCLUSAL.JE_JE_SR_JA_ME_etiqueta(11)).isEqualTo("Incisal");
        assertThat(LINGUAL.JE_JE_SR_JA_ME_etiqueta(26)).isEqualTo("Palatina");
        assertThat(LINGUAL.JE_JE_SR_JA_ME_etiqueta(36)).isEqualTo("Lingual");
        assertThat(JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_espejo(16)).isEqualTo(26);
    }
}
