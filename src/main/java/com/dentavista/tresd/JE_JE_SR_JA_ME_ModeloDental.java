package com.dentavista.tresd;

import com.dentavista.dominio.JE_JE_SR_JA_ME_Pieza;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Convierte el modelo OBJ de la dentadura en un modelo <b>clinico</b>: cada diente
 * identificado con su numero FDI y cada triangulo asignado a una de las cinco caras
 * (oclusal, vestibular, lingual/palatina, mesial y distal).
 * <p>
 * El OBJ es una sola malla sin nombres por diente. El reconocimiento es geometrico:
 * </p>
 * <ol>
 *   <li>Se separa la malla en piezas conexas: 2 encias (las mas anchas) y 28 dientes.</li>
 *   <li>Cada diente se compara con su vecino mas cercano en planta: el que queda mas
 *       arriba pertenece a la arcada superior.</li>
 *   <li>El lado (x &lt; 0 = derecha del paciente) da el cuadrante y la distancia al
 *       frente de la boca da la posicion 1..7.</li>
 *   <li>Los terceros molares (muelas del juicio, 18/28/38/48) no vienen en el modelo:
 *       se generan copiando el segundo molar, desplazado y algo mas pequeno.</li>
 *   <li>Cada triangulo se clasifica segun hacia donde mira respecto a la arcada:
 *       hacia la arcada opuesta = oclusal, hacia afuera = vestibular, hacia la lengua =
 *       lingual, hacia la linea media = mesial y hacia atras = distal.</li>
 * </ol>
 */
public final class JE_JE_SR_JA_ME_ModeloDental {

    /** Geometria lista para dibujar de un diente. */
    public record JE_JE_SR_JA_ME_Diente(int fdi, JE_JE_SR_JA_ME_Malla malla, List<int[]> triangulos, JE_JE_SR_JA_ME_Superficie[] caraDeTriangulo,
                         double[][] normales, double[] centro) {
    }

    /** Geometria de una encia. */
    public record JE_JE_SR_JA_ME_Encia(boolean superior, JE_JE_SR_JA_ME_Malla malla, List<int[]> triangulos, double[][] normales) {
    }

    private static final String RECURSO = "/modelo/dentadura.obj";
    private static JE_JE_SR_JA_ME_ModeloDental instancia;

    private final Map<Integer, JE_JE_SR_JA_ME_Diente> dientes;
    private final List<JE_JE_SR_JA_ME_Encia> encias;
    private final double[] centro;

    private JE_JE_SR_JA_ME_ModeloDental(Map<Integer, JE_JE_SR_JA_ME_Diente> dientes, List<JE_JE_SR_JA_ME_Encia> encias, double[] centro) {
        this.dientes = dientes;
        this.encias = encias;
        this.centro = centro;
    }

    /** Modelo compartido por toda la aplicacion (se procesa una sola vez). */
    public static synchronized JE_JE_SR_JA_ME_ModeloDental JE_JE_SR_JA_ME_get() {
        if (instancia == null) {
            try (InputStream in = Objects.requireNonNull(
                    JE_JE_SR_JA_ME_ModeloDental.class.getResourceAsStream(RECURSO), "Falta el recurso " + RECURSO)) {
                instancia = JE_JE_SR_JA_ME_cargar(in, 2);
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo leer el modelo 3D de la dentadura", e);
            }
        }
        return instancia;
    }

    public Map<Integer, JE_JE_SR_JA_ME_Diente> JE_JE_SR_JA_ME_dientes() {
        return dientes;
    }

    public JE_JE_SR_JA_ME_Diente JE_JE_SR_JA_ME_diente(int fdi) {
        return dientes.get(fdi);
    }

    public List<JE_JE_SR_JA_ME_Encia> JE_JE_SR_JA_ME_encias() {
        return encias;
    }

    /** Centro geometrico de toda la dentadura (coordenadas del OBJ). */
    public double[] JE_JE_SR_JA_ME_centro() {
        return centro;
    }

    // ------------------------------------------------------------------ carga

    /**
     * @param niveles pasos de subdivision Catmull-Clark para los dientes (0 = facetado)
     */
    public static JE_JE_SR_JA_ME_ModeloDental JE_JE_SR_JA_ME_cargar(InputStream obj, int niveles) throws IOException {
        // Se separa por conectividad ANTES de soldar: los dientes tocan la encia y,
        // soldando primero, quedarian unidos a ella. Despues se suelda cada pieza.
        JE_JE_SR_JA_ME_Malla completa = JE_JE_SR_JA_ME_Malla.JE_JE_SR_JA_ME_leerObj(obj);
        List<JE_JE_SR_JA_ME_Malla> piezas = new ArrayList<>();
        for (JE_JE_SR_JA_ME_Malla p : completa.JE_JE_SR_JA_ME_piezasConexas()) {
            piezas.add(p.JE_JE_SR_JA_ME_soldar(1e-4));
        }

        // Las encias son las dos piezas mas anchas.
        piezas.sort(Comparator.comparingDouble(JE_JE_SR_JA_ME_Malla::JE_JE_SR_JA_ME_ancho).reversed());
        List<JE_JE_SR_JA_ME_Malla> enciasCrudas = piezas.subList(0, 2);
        List<JE_JE_SR_JA_ME_Malla> crudos = new ArrayList<>(piezas.subList(2, piezas.size()));

        // ---- 1. Arcada de cada diente (comparando con su vecino en planta).
        int n = crudos.size();
        double[][] c = new double[n][];
        for (int i = 0; i < n; i++) {
            c[i] = crudos.get(i).JE_JE_SR_JA_ME_centro();
        }
        boolean[] superior = new boolean[n];
        for (int i = 0; i < n; i++) {
            int vecino = -1;
            double mejor = Double.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (j == i) {
                    continue;
                }
                double d = JE_JE_SR_JA_ME_sq(c[i][0] - c[j][0]) + JE_JE_SR_JA_ME_sq(c[i][2] - c[j][2]);
                if (d < mejor) {
                    mejor = d;
                    vecino = j;
                }
            }
            superior[i] = c[i][1] > c[vecino][1];
        }

        // ---- 2. Cuadrante y posicion.
        Map<Integer, JE_JE_SR_JA_ME_Malla> porFdi = new TreeMap<>();
        for (int cuadrante = 1; cuadrante <= 4; cuadrante++) {
            boolean sup = cuadrante <= 2;
            boolean derecha = cuadrante == 1 || cuadrante == 4;
            List<Integer> grupo = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (superior[i] == sup && (c[i][0] < 0) == derecha) {
                    grupo.add(i);
                }
            }
            // Del frente (z mayor) hacia atras.
            grupo.sort(Comparator.comparingDouble(i -> -c[i][2]));
            for (int k = 0; k < grupo.size() && k < 8; k++) {
                porFdi.put(cuadrante * 10 + k + 1, crudos.get(grupo.get(k)));
            }
        }

        // ---- 3. Terceros molares sinteticos.
        for (int cuadrante = 1; cuadrante <= 4; cuadrante++) {
            int m7 = cuadrante * 10 + 7;
            int m6 = cuadrante * 10 + 6;
            int m8 = cuadrante * 10 + 8;
            if (!porFdi.containsKey(m8) && porFdi.containsKey(m7) && porFdi.containsKey(m6)) {
                JE_JE_SR_JA_ME_Malla segundo = porFdi.get(m7);
                double[] c7 = segundo.JE_JE_SR_JA_ME_centro();
                double[] c6 = porFdi.get(m6).JE_JE_SR_JA_ME_centro();
                double paso = 0.93;
                double[] c8 = {
                        c7[0] + (c7[0] - c6[0]) * paso,
                        c7[1] + (c7[1] - c6[1]) * paso * 0.5,
                        c7[2] + (c7[2] - c6[2]) * paso};
                porFdi.put(m8, segundo.JE_JE_SR_JA_ME_transformada(c7, c8, 0.9));
            }
        }

        // ---- 4. Subdivision + clasificacion de caras.
        Map<Integer, double[]> centros = new TreeMap<>();
        porFdi.forEach((fdi, m) -> centros.put(fdi, m.JE_JE_SR_JA_ME_centro()));

        Map<Integer, JE_JE_SR_JA_ME_Diente> dientes = new TreeMap<>();
        for (Map.Entry<Integer, JE_JE_SR_JA_ME_Malla> e : porFdi.entrySet()) {
            int fdi = e.getKey();
            JE_JE_SR_JA_ME_Malla m = e.getValue();
            for (int k = 0; k < niveles; k++) {
                m = m.JE_JE_SR_JA_ME_subdividir();
            }
            List<int[]> tris = m.JE_JE_SR_JA_ME_triangulos();
            double[][] normales = m.JE_JE_SR_JA_ME_normalesSuaves(tris);
            JE_JE_SR_JA_ME_Superficie[] caras = JE_JE_SR_JA_ME_clasificar(fdi, m, tris, centros);
            dientes.put(fdi, new JE_JE_SR_JA_ME_Diente(fdi, m, tris, caras, normales, centros.get(fdi)));
        }

        List<JE_JE_SR_JA_ME_Encia> encias = new ArrayList<>();
        for (JE_JE_SR_JA_ME_Malla g : enciasCrudas) {
            JE_JE_SR_JA_ME_Malla m = g.JE_JE_SR_JA_ME_subdividir();
            List<int[]> tris = m.JE_JE_SR_JA_ME_triangulos();
            boolean sup = g.JE_JE_SR_JA_ME_centro()[1] > completa.JE_JE_SR_JA_ME_centro()[1];
            encias.add(new JE_JE_SR_JA_ME_Encia(sup, m, tris, m.JE_JE_SR_JA_ME_normalesSuaves(tris)));
        }

        double[] b = completa.JE_JE_SR_JA_ME_limites();
        double[] centro = {(b[0] + b[1]) / 2, (b[2] + b[3]) / 2, (b[4] + b[5]) / 2};
        return new JE_JE_SR_JA_ME_ModeloDental(dientes, encias, centro);
    }

    // ----------------------------------------------------- caras de cada diente

    private static JE_JE_SR_JA_ME_Superficie[] JE_JE_SR_JA_ME_clasificar(int fdi, JE_JE_SR_JA_ME_Malla m, List<int[]> tris, Map<Integer, double[]> centros) {
        double[] centro = centros.get(fdi);
        boolean sup = JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_esSuperior(fdi);

        // Oclusal: hacia la arcada opuesta.
        double[] oclusal = {0, sup ? -1 : 1, 0};

        // Mesial: a lo largo de la arcada hacia la linea media.
        int pos = JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_posicion(fdi);
        int cuad = JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_cuadrante(fdi);
        double[] anterior = pos == 1 ? centros.get(JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_espejo(fdi)) : centros.get(cuad * 10 + pos - 1);
        double[] posterior = pos == 8 ? centro : centros.get(cuad * 10 + pos + 1);
        if (anterior == null) {
            anterior = centro;
        }
        if (posterior == null) {
            posterior = centro;
        }
        double[] mesial = JE_JE_SR_JA_ME_normalizar(new double[]{anterior[0] - posterior[0], 0, anterior[2] - posterior[2]});

        // Vestibular: perpendicular a la arcada, hacia afuera (lejos del centro de la arcada).
        double[] centroArcada = JE_JE_SR_JA_ME_centroArcada(centros, sup);
        double[] vest = JE_JE_SR_JA_ME_normalizar(new double[]{-mesial[2], 0, mesial[0]});
        double haciaFuera = (centro[0] - centroArcada[0]) * vest[0] + (centro[2] - centroArcada[2]) * vest[2];
        if (haciaFuera < 0) {
            vest = new double[]{-vest[0], 0, -vest[2]};
        }

        double[][] ejes = {vest, JE_JE_SR_JA_ME_neg(vest), mesial, JE_JE_SR_JA_ME_neg(mesial)};
        JE_JE_SR_JA_ME_Superficie[] laterales = {JE_JE_SR_JA_ME_Superficie.VESTIBULAR, JE_JE_SR_JA_ME_Superficie.LINGUAL, JE_JE_SR_JA_ME_Superficie.MESIAL, JE_JE_SR_JA_ME_Superficie.DISTAL};
        double umbralOclusal = JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_esAnterior(fdi) ? 0.72 : 0.55;

        // Altura del diente: la cara oclusal solo puede estar en el tercio masticatorio.
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (int i = 0; i < m.JE_JE_SR_JA_ME_vertices(); i++) {
            double h = m.JE_JE_SR_JA_ME_vertice(i)[1] * oclusal[1];
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        double alturaMinOclusal = min + (max - min) * 0.55;

        JE_JE_SR_JA_ME_Superficie[] resultado = new JE_JE_SR_JA_ME_Superficie[tris.size()];
        for (int t = 0; t < tris.size(); t++) {
            int[] tri = tris.get(t);
            double[] nrm = JE_JE_SR_JA_ME_normalizar(m.JE_JE_SR_JA_ME_normalTriangulo(tri));
            double[] a = m.JE_JE_SR_JA_ME_vertice(tri[0]), b = m.JE_JE_SR_JA_ME_vertice(tri[1]), cc = m.JE_JE_SR_JA_ME_vertice(tri[2]);
            double[] pc = {(a[0] + b[0] + cc[0]) / 3, (a[1] + b[1] + cc[1]) / 3, (a[2] + b[2] + cc[2]) / 3};
            double[] dir = JE_JE_SR_JA_ME_normalizar(new double[]{pc[0] - centro[0], pc[1] - centro[1], pc[2] - centro[2]});

            double altura = pc[1] * oclusal[1];
            if (JE_JE_SR_JA_ME_punto(nrm, oclusal) > umbralOclusal && altura > alturaMinOclusal) {
                resultado[t] = JE_JE_SR_JA_ME_Superficie.OCLUSAL;
                continue;
            }
            double mejor = -Double.MAX_VALUE;
            JE_JE_SR_JA_ME_Superficie elegida = JE_JE_SR_JA_ME_Superficie.VESTIBULAR;
            for (int k = 0; k < ejes.length; k++) {
                double puntaje = 0.55 * JE_JE_SR_JA_ME_punto(nrm, ejes[k]) + 0.45 * JE_JE_SR_JA_ME_punto(dir, ejes[k]);
                if (puntaje > mejor) {
                    mejor = puntaje;
                    elegida = laterales[k];
                }
            }
            resultado[t] = elegida;
        }
        return resultado;
    }

    private static double[] JE_JE_SR_JA_ME_centroArcada(Map<Integer, double[]> centros, boolean superior) {
        double x = 0, z = 0;
        int n = 0;
        for (Map.Entry<Integer, double[]> e : centros.entrySet()) {
            if (JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_esSuperior(e.getKey()) == superior) {
                x += e.getValue()[0];
                z += e.getValue()[2];
                n++;
            }
        }
        return new double[]{x / n, 0, z / n};
    }

    private static double JE_JE_SR_JA_ME_sq(double x) {
        return x * x;
    }

    private static double JE_JE_SR_JA_ME_punto(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] JE_JE_SR_JA_ME_neg(double[] a) {
        return new double[]{-a[0], -a[1], -a[2]};
    }

    private static double[] JE_JE_SR_JA_ME_normalizar(double[] a) {
        double m = Math.sqrt(JE_JE_SR_JA_ME_punto(a, a));
        return m < 1e-12 ? new double[]{0, 1, 0} : new double[]{a[0] / m, a[1] / m, a[2] / m};
    }
}
