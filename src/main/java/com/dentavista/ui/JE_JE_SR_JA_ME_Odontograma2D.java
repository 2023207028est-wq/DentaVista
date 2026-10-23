package com.dentavista.ui;

import com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Odontograma;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Pieza;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Odontograma clínico en 2D (norma de registro en papel): cada pieza con su
 * silueta y el diagrama de cinco caras. Vestibular hacia afuera de la boca,
 * mesial hacia la línea media. Está sincronizado con el visor 3D y también se usa
 * para el PDF.
 */
public class JE_JE_SR_JA_ME_Odontograma2D extends Region {

    private static final double CELDA = 42;
    private static final double CUADRO = 30;
    private static final double INTERIOR = 13;
    private static final double SEPARACION_MEDIA = 18;
    private static final double ALTO_FILA = 126;
    private static final double ANCHO = CELDA * 16 + SEPARACION_MEDIA;
    private static final double ALTO = ALTO_FILA * 2 + 16;

    private static final Color BORDE = Color.web("#94a3b8");
    private static final Color BORDE_HOVER = Color.web("#0d9488");
    private static final Color SANO = Color.WHITE;

    private static final String MOLAR = "M4 20 C3 12 5 4 8 2 C10 6 11 12 12 16 C13 12 14 6 15 4 C16 6 17 12 18 16 "
            + "C19 12 20 6 22 2 C25 4 27 12 26 20 C29 24 29 34 26 40 C22 44 8 44 4 40 C1 34 1 24 4 20 Z";
    private static final String PREMOLAR = "M8 20 C7 12 9 4 15 1 C21 4 23 12 22 20 C26 24 26 34 24 40 "
            + "C20 44 10 44 6 40 C4 34 4 24 8 20 Z";
    private static final String CANINO = "M10 20 C9 10 12 2 15 0 C18 2 21 10 20 20 C23 25 23 34 20 40 "
            + "C18 43 16 44 15 44 C14 44 12 43 10 40 C7 34 7 25 10 20 Z";
    private static final String INCISIVO = "M10 22 C9 12 12 3 15 1 C18 3 21 12 20 22 C23 26 23 36 21 42 "
            + "C18 44 12 44 9 42 C7 36 7 26 10 22 Z";

    private final Group lienzo = new Group();
    private final Map<Integer, JE_JE_SR_JA_ME_Celda> celdas = new HashMap<>();

    private JE_JE_SR_JA_ME_Odontograma odontograma = JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_vacio();
    private Integer seleccionada;
    private boolean interactivo = true;
    private BiConsumer<Integer, JE_JE_SR_JA_ME_Superficie> alPulsar = (p, s) -> { };
    private BiConsumer<Integer, JE_JE_SR_JA_ME_Superficie> alDobleClic = (p, s) -> { };
    private BiConsumer<Integer, JE_JE_SR_JA_ME_Superficie> alPasar = (p, s) -> { };

    public JE_JE_SR_JA_ME_Odontograma2D() {
        getStyleClass().add("odontograma-2d");
        JE_JE_SR_JA_ME_construir();
        getChildren().add(lienzo);
        JE_JE_SR_JA_ME_repintarTodo();
    }

    // ------------------------------------------------------------ construcción

    private void JE_JE_SR_JA_ME_construir() {
        Line media = new Line(ANCHO / 2, 4, ANCHO / 2, ALTO - 4);
        media.setStroke(Color.web("#cbd5e1"));
        media.getStrokeDashArray().addAll(4.0, 4.0);
        Line horizontal = new Line(4, ALTO / 2, ANCHO - 4, ALTO / 2);
        horizontal.setStroke(Color.web("#e2e8f0"));
        lienzo.getChildren().addAll(media, horizontal);

        JE_JE_SR_JA_ME_colocarFila(JE_JE_SR_JA_ME_Pieza.FILA_SUPERIOR, 0, true);
        JE_JE_SR_JA_ME_colocarFila(JE_JE_SR_JA_ME_Pieza.FILA_INFERIOR, ALTO_FILA + 16, false);
    }

    private void JE_JE_SR_JA_ME_colocarFila(List<Integer> fila, double y, boolean superior) {
        for (int i = 0; i < fila.size(); i++) {
            int fdi = fila.get(i);
            double x = i * CELDA + (i >= 8 ? SEPARACION_MEDIA : 0);
            JE_JE_SR_JA_ME_Celda c = new JE_JE_SR_JA_ME_Celda(fdi, superior);
            c.raiz.setLayoutX(x);
            c.raiz.setLayoutY(y);
            celdas.put(fdi, c);
            lienzo.getChildren().add(c.raiz);
        }
    }

    // --------------------------------------------------------------- escala

    @Override
    protected void layoutChildren() {
        double w = getWidth() - snappedLeftInset() - snappedRightInset();
        double h = getHeight() - snappedTopInset() - snappedBottomInset();
        double escala = Math.min(w / ANCHO, h / ALTO);
        escala = Math.max(0.3, Math.min(escala, 1.6));
        lienzo.setScaleX(escala);
        lienzo.setScaleY(escala);
        Bounds b = lienzo.getLayoutBounds();
        double anchoReal = ANCHO * escala;
        double altoReal = ALTO * escala;
        // El escalado de un Group es respecto a su centro: se compensa para alinearlo.
        lienzo.setLayoutX(snappedLeftInset() + (w - anchoReal) / 2 - b.getMinX() - (b.getWidth() - anchoReal) / 2);
        lienzo.setLayoutY(snappedTopInset() + (h - altoReal) / 2 - b.getMinY() - (b.getHeight() - altoReal) / 2);
    }

    @Override
    protected double computePrefWidth(double height) {
        return ANCHO + snappedLeftInset() + snappedRightInset();
    }

    @Override
    protected double computePrefHeight(double width) {
        double w = width > 0 ? width : ANCHO;
        return Math.min(ALTO, ALTO * (w / ANCHO)) + snappedTopInset() + snappedBottomInset();
    }

    @Override
    protected double computeMinWidth(double height) {
        return 320;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 120;
    }

    // ------------------------------------------------------------------ API

    public void JE_JE_SR_JA_ME_setOdontograma(JE_JE_SR_JA_ME_Odontograma o) {
        this.odontograma = o == null ? JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_vacio() : o;
        JE_JE_SR_JA_ME_repintarTodo();
    }

    public void JE_JE_SR_JA_ME_seleccionar(Integer fdi) {
        Integer anterior = seleccionada;
        seleccionada = fdi;
        if (anterior != null) {
            celdas.get(anterior).JE_JE_SR_JA_ME_repintar();
        }
        if (fdi != null) {
            celdas.get(fdi).JE_JE_SR_JA_ME_repintar();
        }
    }

    public void JE_JE_SR_JA_ME_setInteractivo(boolean interactivo) {
        this.interactivo = interactivo;
    }

    public void JE_JE_SR_JA_ME_setAlPulsar(BiConsumer<Integer, JE_JE_SR_JA_ME_Superficie> accion) {
        this.alPulsar = accion;
    }

    /** Doble clic sobre una cara o la silueta: (fdi, cara o PIEZA). */
    public void JE_JE_SR_JA_ME_setAlDobleClic(BiConsumer<Integer, JE_JE_SR_JA_ME_Superficie> accion) {
        this.alDobleClic = accion;
    }

    public void JE_JE_SR_JA_ME_setAlPasar(BiConsumer<Integer, JE_JE_SR_JA_ME_Superficie> accion) {
        this.alPasar = accion;
    }

    private void JE_JE_SR_JA_ME_repintarTodo() {
        celdas.values().forEach(JE_JE_SR_JA_ME_Celda::JE_JE_SR_JA_ME_repintar);
    }

    // -------------------------------------------------- diagrama de una pieza

    /**
     * Diagrama de cinco caras de una sola pieza, en grande (panel lateral de la ficha).
     */
    public static Group JE_JE_SR_JA_ME_diagramaPieza(int fdi, JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_EstadoPieza estado, double tamano,
                                      JE_JE_SR_JA_ME_Superficie resaltada, BiConsumer<Integer, JE_JE_SR_JA_ME_Superficie> alPulsar) {
        return JE_JE_SR_JA_ME_diagramaPieza(fdi, estado, tamano, resaltada, alPulsar, null);
    }

    /** Igual que el anterior, con acción de doble clic (volver la cara a sana). */
    public static Group JE_JE_SR_JA_ME_diagramaPieza(int fdi, JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_EstadoPieza estado, double tamano, JE_JE_SR_JA_ME_Superficie resaltada,
                                      BiConsumer<Integer, JE_JE_SR_JA_ME_Superficie> alPulsar, BiConsumer<Integer, JE_JE_SR_JA_ME_Superficie> alDoble) {
        Map<JE_JE_SR_JA_ME_Superficie, Polygon> caras = JE_JE_SR_JA_ME_poligonos(fdi, JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_esSuperior(fdi), tamano, tamano * INTERIOR / CUADRO);
        Group g = new Group();
        caras.forEach((s, p) -> {
            JE_JE_SR_JA_ME_EstadoDental e = estado.JE_JE_SR_JA_ME_visible(s);
            p.setFill(e == JE_JE_SR_JA_ME_EstadoDental.SANO ? SANO : Color.web(e.JE_JE_SR_JA_ME_getColor()));
            p.setStroke(s == resaltada ? BORDE_HOVER : BORDE);
            p.setStrokeWidth(s == resaltada ? 3 : 1.2);
            if (alPulsar != null) {
                p.setCursor(Cursor.HAND);
                p.setOnMouseClicked(ev -> {
                    if (ev.getClickCount() == 2 && alDoble != null) {
                        alDoble.accept(fdi, s);
                    } else {
                        alPulsar.accept(fdi, s);
                    }
                });
                p.setOnMouseEntered(ev -> {
                    p.setStroke(BORDE_HOVER);
                    p.setStrokeWidth(3);
                    p.toFront();
                });
                p.setOnMouseExited(ev -> {
                    p.setStroke(s == resaltada ? BORDE_HOVER : BORDE);
                    p.setStrokeWidth(s == resaltada ? 3 : 1.2);
                });
            }
            g.getChildren().add(p);
        });
        g.getChildren().add(JE_JE_SR_JA_ME_simbolo(estado.JE_JE_SR_JA_ME_general(), tamano, 0, 0));
        return g;
    }

    /**
     * Polígonos de las cinco caras dentro de un cuadrado de lado {@code s}: arriba,
     * derecha, abajo, izquierda (trapecios) y centro (oclusal).
     */
    private static Map<JE_JE_SR_JA_ME_Superficie, Polygon> JE_JE_SR_JA_ME_poligonos(int fdi, boolean superior, double s, double interior) {
        double a = (s - interior) / 2;
        double b = (s + interior) / 2;
        Polygon arriba = new Polygon(0, 0, s, 0, b, a, a, a);
        Polygon derecha = new Polygon(s, 0, s, s, b, b, b, a);
        Polygon abajo = new Polygon(s, s, 0, s, a, b, b, b);
        Polygon izquierda = new Polygon(0, s, 0, 0, a, a, a, b);
        Polygon centro = new Polygon(a, a, b, a, b, b, a, b);

        // Mesial hacia la línea media: en la mitad izquierda del gráfico es la derecha.
        boolean mitadIzquierda = JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_esDerecha(fdi);
        Map<JE_JE_SR_JA_ME_Superficie, Polygon> m = new EnumMap<>(JE_JE_SR_JA_ME_Superficie.class);
        m.put(JE_JE_SR_JA_ME_Superficie.VESTIBULAR, superior ? arriba : abajo);
        m.put(JE_JE_SR_JA_ME_Superficie.LINGUAL, superior ? abajo : arriba);
        m.put(JE_JE_SR_JA_ME_Superficie.MESIAL, mitadIzquierda ? derecha : izquierda);
        m.put(JE_JE_SR_JA_ME_Superficie.DISTAL, mitadIzquierda ? izquierda : derecha);
        m.put(JE_JE_SR_JA_ME_Superficie.OCLUSAL, centro);
        for (Polygon p : m.values()) {
            p.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        }
        return m;
    }

    /** Símbolo clínico de los estados de pieza completa sobre el cuadro de caras. */
    private static Group JE_JE_SR_JA_ME_simbolo(JE_JE_SR_JA_ME_EstadoDental general, double s, double x, double y) {
        Group g = new Group();
        g.setMouseTransparent(true);
        if (general == null) {
            return g;
        }
        Color c = Color.web(general.JE_JE_SR_JA_ME_getColor());
        switch (general) {
            case AUSENTE, EXTRACCION_INDICADA -> {
                Color color = general == JE_JE_SR_JA_ME_EstadoDental.AUSENTE ? Color.web("#2563eb") : c;
                g.getChildren().addAll(JE_JE_SR_JA_ME_linea(x - 3, y - 3, x + s + 3, y + s + 3, color, 3.2),
                        JE_JE_SR_JA_ME_linea(x + s + 3, y - 3, x - 3, y + s + 3, color, 3.2));
            }
            case CORONA -> {
                Circle ci = new Circle(x + s / 2, y + s / 2, s * 0.68);
                ci.setFill(Color.TRANSPARENT);
                ci.setStroke(c);
                ci.setStrokeWidth(3);
                g.getChildren().add(ci);
            }
            case ENDODONCIA, ENDODONCIA_INDICADA ->
                    g.getChildren().add(JE_JE_SR_JA_ME_linea(x + s / 2, y - s * 0.35, x + s / 2, y + s * 1.35, c, 3.4));
            case IMPLANTE -> {
                Rectangle r = new Rectangle(x - 2, y - 2, s + 4, s + 4);
                r.setFill(Color.TRANSPARENT);
                r.setStroke(c);
                r.setStrokeWidth(3);
                r.setArcWidth(6);
                r.setArcHeight(6);
                Text t = new Text("IMP");
                t.setFont(Font.font("Segoe UI", FontWeight.BOLD, s * 0.32));
                t.setFill(Color.web("#334155"));
                t.setX(x + s / 2 - t.getLayoutBounds().getWidth() / 2);
                t.setY(y + s / 2 + t.getLayoutBounds().getHeight() / 3.2);
                g.getChildren().addAll(r, t);
            }
            default -> {
            }
        }
        return g;
    }

    private static Line JE_JE_SR_JA_ME_linea(double x1, double y1, double x2, double y2, Color c, double grosor) {
        Line l = new Line(x1, y1, x2, y2);
        l.setStroke(c);
        l.setStrokeWidth(grosor);
        l.setStrokeLineCap(StrokeLineCap.ROUND);
        return l;
    }

    private static String JE_JE_SR_JA_ME_silueta(int fdi) {
        int pos = JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_posicion(fdi);
        if (pos >= 6) {
            return MOLAR;
        }
        if (pos >= 4) {
            return PREMOLAR;
        }
        return pos == 3 ? CANINO : INCISIVO;
    }

    // ------------------------------------------------------------------ celda

    /** Una pieza del odontograma: número, silueta y diagrama de caras. */
    private final class JE_JE_SR_JA_ME_Celda {

        private final int fdi;
        private final Group raiz = new Group();
        private final Rectangle fondo = new Rectangle(CELDA - 2, ALTO_FILA - 4);
        private final SVGPath forma = new SVGPath();
        private final Map<JE_JE_SR_JA_ME_Superficie, Polygon> caras;
        private final Group capaSimbolo = new Group();
        private final Text numero = new Text();
        private final double xCuadro;
        private final double yCuadro;
        private JE_JE_SR_JA_ME_Superficie hover;

        JE_JE_SR_JA_ME_Celda(int fdi, boolean superior) {
            this.fdi = fdi;
            xCuadro = (CELDA - CUADRO) / 2;

            fondo.setArcWidth(12);
            fondo.setArcHeight(12);
            fondo.setX(1);
            fondo.setY(2);
            fondo.setFill(Color.TRANSPARENT);

            numero.setText(String.valueOf(fdi));
            numero.getStyleClass().add("o2d-numero");
            numero.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            numero.setFill(Color.web("#475569"));
            numero.setX(CELDA / 2 - numero.getLayoutBounds().getWidth() / 2);

            forma.setContent(JE_JE_SR_JA_ME_silueta(fdi));
            forma.setStrokeWidth(1.1);
            // La silueta mide 30x44: se encaja centrada en la celda.
            forma.setLayoutX((CELDA - 30) / 2);
            if (superior) {
                numero.setY(16);
                forma.setLayoutY(22);
                yCuadro = 76;
            } else {
                // Arcada inferior: raíz hacia abajo (silueta invertida).
                yCuadro = 10;
                forma.setScaleY(-1);
                forma.setLayoutY(48);
                numero.setY(ALTO_FILA - 8);
            }
            caras = JE_JE_SR_JA_ME_poligonos(fdi, superior, CUADRO, INTERIOR);
            Group cuadro = new Group();
            caras.forEach((s, p) -> {
                p.setStroke(BORDE);
                p.setStrokeWidth(1);
                p.setOnMouseEntered(e -> JE_JE_SR_JA_ME_entrar(s));
                p.setOnMouseExited(e -> JE_JE_SR_JA_ME_salir());
                p.setOnMouseClicked(e -> {
                    if (interactivo) {
                        (e.getClickCount() == 2 ? alDobleClic : alPulsar).accept(fdi, s);
                    }
                });
                cuadro.getChildren().add(p);
            });
            cuadro.setLayoutX(xCuadro);
            cuadro.setLayoutY(yCuadro);
            capaSimbolo.setLayoutX(xCuadro);
            capaSimbolo.setLayoutY(yCuadro);

            forma.setOnMouseEntered(e -> JE_JE_SR_JA_ME_entrar(JE_JE_SR_JA_ME_Superficie.PIEZA));
            forma.setOnMouseExited(e -> JE_JE_SR_JA_ME_salir());
            forma.setOnMouseClicked(e -> {
                if (interactivo) {
                    (e.getClickCount() == 2 ? alDobleClic : alPulsar).accept(fdi, JE_JE_SR_JA_ME_Superficie.PIEZA);
                }
            });

            raiz.getChildren().addAll(fondo, forma, cuadro, capaSimbolo, numero);
            raiz.setCursor(Cursor.HAND);
        }

        private void JE_JE_SR_JA_ME_entrar(JE_JE_SR_JA_ME_Superficie s) {
            if (!interactivo) {
                return;
            }
            hover = s;
            JE_JE_SR_JA_ME_repintar();
            alPasar.accept(fdi, s);
        }

        private void JE_JE_SR_JA_ME_salir() {
            hover = null;
            JE_JE_SR_JA_ME_repintar();
            alPasar.accept(null, null);
        }

        void JE_JE_SR_JA_ME_repintar() {
            boolean sel = seleccionada != null && seleccionada == fdi;
            fondo.setFill(sel ? Color.web("#14b8a6", 0.16) : hover != null ? Color.web("#94a3b8", 0.12) : Color.TRANSPARENT);
            fondo.setStroke(sel ? Color.web("#14b8a6") : Color.TRANSPARENT);

            JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_EstadoPieza e = odontograma.JE_JE_SR_JA_ME_pieza(fdi);
            boolean ausente = e.JE_JE_SR_JA_ME_ausente();
            JE_JE_SR_JA_ME_EstadoDental general = e.JE_JE_SR_JA_ME_general();
            if (ausente) {
                forma.setFill(Color.TRANSPARENT);
                forma.setStroke(Color.web("#94a3b8"));
                forma.getStrokeDashArray().setAll(3.0, 3.0);
            } else {
                forma.getStrokeDashArray().clear();
                forma.setFill(general != null ? Color.web(general.JE_JE_SR_JA_ME_getColor()).deriveColor(0, 0.9, 1.05, 0.9)
                        : Color.web("#f8f5ee"));
                forma.setStroke(hover == JE_JE_SR_JA_ME_Superficie.PIEZA ? BORDE_HOVER : Color.web("#a8b3c4"));
                forma.setStrokeWidth(hover == JE_JE_SR_JA_ME_Superficie.PIEZA ? 2.2 : 1.1);
            }
            caras.forEach((s, p) -> {
                JE_JE_SR_JA_ME_EstadoDental v = e.JE_JE_SR_JA_ME_visible(s);
                p.setFill(ausente ? Color.web("#f1f5f9") : v == JE_JE_SR_JA_ME_EstadoDental.SANO ? SANO : Color.web(v.JE_JE_SR_JA_ME_getColor()));
                boolean resalta = hover == s || hover == JE_JE_SR_JA_ME_Superficie.PIEZA;
                p.setStroke(resalta ? BORDE_HOVER : BORDE);
                p.setStrokeWidth(resalta ? 2.2 : 1);
                if (hover == s) {
                    p.toFront();
                }
            });
            capaSimbolo.getChildren().setAll(JE_JE_SR_JA_ME_simbolo(general, CUADRO, 0, 0));
        }

    }

    /** Nodo raíz dibujado (para capturas sin escalar). */
    public Node JE_JE_SR_JA_ME_lienzo() {
        return lienzo;
    }
}
