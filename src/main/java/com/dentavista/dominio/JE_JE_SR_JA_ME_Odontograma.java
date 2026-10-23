package com.dentavista.dominio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Estado completo de la boca de un paciente en un instante dado.
 * <p>
 * No se guarda "el odontograma" como tal: se guardan los <b>registros clinicos</b>
 * (hallazgos con fecha) y el odontograma se <b>reconstruye</b> aplicandolos en orden
 * cronologico. Asi se puede ver la boca del paciente en cualquier fecha del pasado
 * (la linea de tiempo de la ficha) sin duplicar datos.
 * </p>
 * Reglas al aplicar un registro:
 * <ul>
 *   <li>Registro de <b>cara</b>: fija el estado de esa cara; {@code SANO} la limpia.</li>
 *   <li>Registro de <b>pieza</b>: fija el estado general; {@code SANO} restablece la pieza
 *       entera. Ausente, implante y corona borran las caras porque ya no existen.</li>
 * </ul>
 */
public final class JE_JE_SR_JA_ME_Odontograma {

    /** Un hallazgo clinico, independiente de la persistencia. */
    public record JE_JE_SR_JA_ME_Registro(int pieza, JE_JE_SR_JA_ME_Superficie superficie, JE_JE_SR_JA_ME_EstadoDental estado,
                           LocalDateTime fecha, long secuencia) {
        public JE_JE_SR_JA_ME_Registro {
            JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_validar(pieza);
        }
    }

    /** Una patologia pendiente de tratar (entra al plan de tratamiento). */
    public record JE_JE_SR_JA_ME_Patologia(int pieza, JE_JE_SR_JA_ME_Superficie superficie, JE_JE_SR_JA_ME_EstadoDental estado) {
    }

    private final Map<Integer, JE_JE_SR_JA_ME_EstadoPieza> piezas = new TreeMap<>();

    private JE_JE_SR_JA_ME_Odontograma() {
        for (int fdi : JE_JE_SR_JA_ME_Pieza.TODAS) {
            piezas.put(fdi, new JE_JE_SR_JA_ME_EstadoPieza(fdi));
        }
    }

    /** Odontograma sano, sin registros. */
    public static JE_JE_SR_JA_ME_Odontograma JE_JE_SR_JA_ME_vacio() {
        return new JE_JE_SR_JA_ME_Odontograma();
    }

    /** Reconstruye la boca con todos los registros hasta {@code hasta} inclusive (null = todos). */
    public static JE_JE_SR_JA_ME_Odontograma JE_JE_SR_JA_ME_reconstruir(Collection<JE_JE_SR_JA_ME_Registro> registros, LocalDateTime hasta) {
        JE_JE_SR_JA_ME_Odontograma o = new JE_JE_SR_JA_ME_Odontograma();
        registros.stream()
                .filter(r -> hasta == null || !r.fecha().isAfter(hasta))
                .sorted(Comparator.comparing(JE_JE_SR_JA_ME_Registro::fecha).thenComparingLong(JE_JE_SR_JA_ME_Registro::secuencia))
                .forEach(o::JE_JE_SR_JA_ME_aplicar);
        return o;
    }

    public static JE_JE_SR_JA_ME_Odontograma JE_JE_SR_JA_ME_reconstruir(Collection<JE_JE_SR_JA_ME_Registro> registros) {
        return JE_JE_SR_JA_ME_reconstruir(registros, null);
    }

    private void JE_JE_SR_JA_ME_aplicar(JE_JE_SR_JA_ME_Registro r) {
        piezas.get(r.pieza()).JE_JE_SR_JA_ME_aplicar(r.superficie(), r.estado());
    }

    public JE_JE_SR_JA_ME_EstadoPieza JE_JE_SR_JA_ME_pieza(int fdi) {
        JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_validar(fdi);
        return piezas.get(fdi);
    }

    public Collection<JE_JE_SR_JA_ME_EstadoPieza> JE_JE_SR_JA_ME_piezas() {
        return Collections.unmodifiableCollection(piezas.values());
    }

    /** Todo lo que requiere tratamiento, en orden de pieza. */
    public List<JE_JE_SR_JA_ME_Patologia> JE_JE_SR_JA_ME_patologias() {
        List<JE_JE_SR_JA_ME_Patologia> lista = new ArrayList<>();
        for (JE_JE_SR_JA_ME_EstadoPieza p : piezas.values()) {
            if (p.JE_JE_SR_JA_ME_general() != null && p.JE_JE_SR_JA_ME_general().JE_JE_SR_JA_ME_esPatologia()) {
                lista.add(new JE_JE_SR_JA_ME_Patologia(p.JE_JE_SR_JA_ME_fdi(), JE_JE_SR_JA_ME_Superficie.PIEZA, p.JE_JE_SR_JA_ME_general()));
            }
            p.caras.forEach((cara, estado) -> {
                if (estado.JE_JE_SR_JA_ME_esPatologia()) {
                    lista.add(new JE_JE_SR_JA_ME_Patologia(p.JE_JE_SR_JA_ME_fdi(), cara, estado));
                }
            });
        }
        return lista;
    }

    /** Cuantas piezas presentan cada estado (para indicadores). */
    public Map<JE_JE_SR_JA_ME_EstadoDental, Integer> JE_JE_SR_JA_ME_resumen() {
        Map<JE_JE_SR_JA_ME_EstadoDental, Integer> conteo = new EnumMap<>(JE_JE_SR_JA_ME_EstadoDental.class);
        for (JE_JE_SR_JA_ME_EstadoPieza p : piezas.values()) {
            if (p.JE_JE_SR_JA_ME_general() != null) {
                conteo.merge(p.JE_JE_SR_JA_ME_general(), 1, Integer::sum);
            }
            p.caras.values().stream().distinct()
                    .forEach(e -> conteo.merge(e, 1, Integer::sum));
        }
        return conteo;
    }

    /** Piezas presentes en boca (no ausentes ni extraidas). */
    public int JE_JE_SR_JA_ME_piezasPresentes() {
        return (int) piezas.values().stream().filter(p -> !p.JE_JE_SR_JA_ME_ausente()).count();
    }

    /** Estado de una pieza: estado general y estado por cara. */
    public static final class JE_JE_SR_JA_ME_EstadoPieza {

        private final int fdi;
        private JE_JE_SR_JA_ME_EstadoDental general;
        private final EnumMap<JE_JE_SR_JA_ME_Superficie, JE_JE_SR_JA_ME_EstadoDental> caras = new EnumMap<>(JE_JE_SR_JA_ME_Superficie.class);

        private JE_JE_SR_JA_ME_EstadoPieza(int fdi) {
            this.fdi = fdi;
        }

        private void JE_JE_SR_JA_ME_aplicar(JE_JE_SR_JA_ME_Superficie superficie, JE_JE_SR_JA_ME_EstadoDental estado) {
            if (superficie == JE_JE_SR_JA_ME_Superficie.PIEZA) {
                if (estado == JE_JE_SR_JA_ME_EstadoDental.SANO) {
                    general = null;
                    caras.clear();
                    return;
                }
                general = estado;
                if (estado.JE_JE_SR_JA_ME_anulaCaras()) {
                    caras.clear();
                }
                return;
            }
            if (estado == JE_JE_SR_JA_ME_EstadoDental.SANO) {
                caras.remove(superficie);
            } else {
                caras.put(superficie, estado);
            }
        }

        public int JE_JE_SR_JA_ME_fdi() {
            return fdi;
        }

        /** Estado de la pieza completa, o null si no tiene ninguno. */
        public JE_JE_SR_JA_ME_EstadoDental JE_JE_SR_JA_ME_general() {
            return general;
        }

        /** Estado registrado en una cara concreta, o null. */
        public JE_JE_SR_JA_ME_EstadoDental JE_JE_SR_JA_ME_cara(JE_JE_SR_JA_ME_Superficie s) {
            return caras.get(s);
        }

        public Map<JE_JE_SR_JA_ME_Superficie, JE_JE_SR_JA_ME_EstadoDental> JE_JE_SR_JA_ME_caras() {
            return Collections.unmodifiableMap(caras);
        }

        /**
         * Estado con el que se <b>pinta</b> una cara: si la pieza esta ausente, con
         * implante o corona se pinta toda de ese color; si no, la cara usa su propio
         * hallazgo y, sin hallazgo, el estado general (p. ej. endodoncia) o sano.
         */
        public JE_JE_SR_JA_ME_EstadoDental JE_JE_SR_JA_ME_visible(JE_JE_SR_JA_ME_Superficie s) {
            if (general != null && general.JE_JE_SR_JA_ME_anulaCaras()) {
                return general;
            }
            JE_JE_SR_JA_ME_EstadoDental propio = caras.get(s);
            if (propio != null) {
                return propio;
            }
            return general != null ? general : JE_JE_SR_JA_ME_EstadoDental.SANO;
        }

        public boolean JE_JE_SR_JA_ME_ausente() {
            return general == JE_JE_SR_JA_ME_EstadoDental.AUSENTE;
        }

        public boolean JE_JE_SR_JA_ME_sana() {
            return general == null && caras.isEmpty();
        }

        /** Texto corto para listas: "Caries O, M · Endodoncia realizada". */
        public String JE_JE_SR_JA_ME_descripcion() {
            if (JE_JE_SR_JA_ME_sana()) {
                return "Sana";
            }
            List<String> partes = new ArrayList<>();
            Map<JE_JE_SR_JA_ME_EstadoDental, List<String>> porEstado = new EnumMap<>(JE_JE_SR_JA_ME_EstadoDental.class);
            caras.forEach((c, e) -> porEstado.computeIfAbsent(e, k -> new ArrayList<>()).add(c.JE_JE_SR_JA_ME_sigla(fdi)));
            porEstado.forEach((e, siglas) -> partes.add(e.JE_JE_SR_JA_ME_getEtiqueta() + " " + String.join(", ", siglas)));
            if (general != null) {
                partes.add(general.JE_JE_SR_JA_ME_getEtiqueta());
            }
            return String.join(" · ", partes);
        }
    }
}
