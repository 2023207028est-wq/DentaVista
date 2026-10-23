package com.dentavista.tresd;

import com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Odontograma;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Pieza;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Odontograma tridimensional interactivo.
 * <ul>
 *   <li><b>Arrastrar</b> con el boton izquierdo: girar la dentadura.</li>
 *   <li><b>Rueda</b>: acercar / alejar. <b>Boton derecho</b> arrastrando: desplazar.</li>
 *   <li><b>Clic</b> sobre un diente: informa la pieza FDI y la cara exacta pulsada.</li>
 *   <li><b>Doble clic</b> en el fondo: vuelve a la vista inicial.</li>
 * </ul>
 * Cada cara de cada diente es una malla propia, de modo que se puede colorear una
 * caries solo en la cara oclusal de un molar, igual que en un odontograma de papel.
 */
public class JE_JE_SR_JA_ME_VisorDental3D extends StackPane {

    /** Escala de centimetros del OBJ a unidades de pantalla. */
    private static final double ESCALA = 22;
    private static final double DISTANCIA_INICIAL = 740;
    private static final double INCLINACION_INICIAL = -10;

    private final JE_JE_SR_JA_ME_ModeloDental modelo = JE_JE_SR_JA_ME_ModeloDental.JE_JE_SR_JA_ME_get();

    private final Group mundo = new Group();
    private final Group arcoSuperior = new Group();
    private final Group arcoInferior = new Group();
    private final Rotate giroX = new Rotate(INCLINACION_INICIAL, Rotate.X_AXIS);
    private final Rotate giroY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate desplazamiento = new Translate();
    private final Rotate aperturaSup = new Rotate(0, Rotate.X_AXIS);
    private final Rotate aperturaInf = new Rotate(0, Rotate.X_AXIS);
    private final Translate separacionSup = new Translate();
    private final Translate separacionInf = new Translate();
    private final PerspectiveCamera camara = new PerspectiveCamera(true);
    private final SubScene subEscena;

    private final Map<Integer, Group> nodosDiente = new HashMap<>();
    private final Map<Integer, Map<JE_JE_SR_JA_ME_Superficie, MeshView>> caras = new HashMap<>();
    private final Map<Integer, Point3D> centros = new HashMap<>();
    private final Map<String, PhongMaterial> materiales = new HashMap<>();

    private final Pane capaEtiquetas = new Pane();
    private final Map<Integer, Label> numeros = new HashMap<>();
    private final Label etiquetaHover = new Label();
    private final BooleanProperty numerosVisibles = new SimpleBooleanProperty(true);
    private final BooleanProperty giroAutomatico = new SimpleBooleanProperty(false);
    private final BooleanProperty bocaAbierta = new SimpleBooleanProperty(false);

    private JE_JE_SR_JA_ME_Odontograma odontograma = JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_vacio();
    private Integer seleccionada;
    private Integer hoverPieza;
    private JE_JE_SR_JA_ME_Superficie hoverCara;
    private boolean resaltarCara = true;

    private BiConsumer<Integer, JE_JE_SR_JA_ME_Superficie> alPulsar = (p, s) -> { };
    private BiConsumer<Integer, JE_JE_SR_JA_ME_Superficie> alDobleClic = (p, s) -> { };
    private BiConsumer<Integer, JE_JE_SR_JA_ME_Superficie> alPasar = (p, s) -> { };

    private double ratonX, ratonY, inicioX, inicioY;
    private boolean arrastrando;
    private Timeline animacionVista;

    public JE_JE_SR_JA_ME_VisorDental3D() {
        getStyleClass().add("visor-3d");
        JE_JE_SR_JA_ME_construirDientes();
        JE_JE_SR_JA_ME_construirEncias();

        mundo.getChildren().addAll(arcoSuperior, arcoInferior);
        mundo.getTransforms().addAll(desplazamiento, giroX, giroY);

        // Bisagra de apertura: en la parte posterior de la boca (z positivo tras el giro).
        double bisagraZ = centros.values().stream().mapToDouble(Point3D::getZ).max().orElse(0);
        aperturaSup.setPivotZ(bisagraZ);
        aperturaInf.setPivotZ(bisagraZ);
        arcoSuperior.getTransforms().addAll(separacionSup, aperturaSup);
        arcoInferior.getTransforms().addAll(separacionInf, aperturaInf);

        AmbientLight ambiente = new AmbientLight(Color.rgb(78, 80, 90));
        PointLight principal = new PointLight(Color.rgb(226, 220, 210));
        principal.getTransforms().add(new Translate(-450, -620, -900));
        PointLight relleno = new PointLight(Color.rgb(78, 98, 128));
        relleno.getTransforms().add(new Translate(650, 250, -500));
        PointLight contra = new PointLight(Color.rgb(70, 70, 88));
        contra.getTransforms().add(new Translate(0, -300, 900));

        camara.setNearClip(1);
        camara.setFarClip(10000);
        camara.setFieldOfView(30);
        camara.setTranslateZ(-DISTANCIA_INICIAL);

        Group raiz = new Group(mundo, ambiente, principal, relleno, contra, camara);
        subEscena = new SubScene(raiz, 800, 600, true, SceneAntialiasing.BALANCED);
        subEscena.setCamera(camara);
        subEscena.setFill(Color.TRANSPARENT);
        subEscena.widthProperty().bind(widthProperty());
        subEscena.heightProperty().bind(heightProperty());

        etiquetaHover.getStyleClass().add("visor-hover");
        etiquetaHover.setVisible(false);
        etiquetaHover.setManaged(false);
        etiquetaHover.setMouseTransparent(true);
        capaEtiquetas.setMouseTransparent(true);
        capaEtiquetas.getChildren().add(etiquetaHover);
        capaEtiquetas.visibleProperty().bind(numerosVisibles.or(etiquetaHover.visibleProperty()));

        getChildren().addAll(subEscena, capaEtiquetas);
        setMinSize(200, 200);

        JE_JE_SR_JA_ME_instalarRaton();
        JE_JE_SR_JA_ME_instalarBucle();
        JE_JE_SR_JA_ME_repintarTodo();
    }

    // ------------------------------------------------------------ construccion

    private void JE_JE_SR_JA_ME_construirDientes() {
        double[] c = modelo.JE_JE_SR_JA_ME_centro();
        for (JE_JE_SR_JA_ME_ModeloDental.JE_JE_SR_JA_ME_Diente d : modelo.JE_JE_SR_JA_ME_dientes().values()) {
            float[] puntos = JE_JE_SR_JA_ME_aPuntos(d.malla(), c);
            float[] normales = JE_JE_SR_JA_ME_aNormales(d.normales());

            Map<JE_JE_SR_JA_ME_Superficie, List<int[]>> porCara = new EnumMap<>(JE_JE_SR_JA_ME_Superficie.class);
            for (int t = 0; t < d.triangulos().size(); t++) {
                porCara.computeIfAbsent(d.caraDeTriangulo()[t], k -> new ArrayList<>()).add(d.triangulos().get(t));
            }

            Group g = new Group();
            Map<JE_JE_SR_JA_ME_Superficie, MeshView> mapa = new EnumMap<>(JE_JE_SR_JA_ME_Superficie.class);
            porCara.forEach((cara, tris) -> {
                MeshView mv = new MeshView(JE_JE_SR_JA_ME_malla(puntos, normales, tris));
                mv.setCullFace(CullFace.BACK);
                mv.setUserData(new Object[]{d.fdi(), cara});
                mapa.put(cara, mv);
                g.getChildren().add(mv);
            });
            g.setUserData(d.fdi());
            nodosDiente.put(d.fdi(), g);
            caras.put(d.fdi(), mapa);
            double[] dc = d.centro();
            centros.put(d.fdi(), new Point3D((dc[0] - c[0]) * ESCALA, -(dc[1] - c[1]) * ESCALA, -(dc[2] - c[2]) * ESCALA));
            Group arco = JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_esSuperior(d.fdi()) ? arcoSuperior : arcoInferior;
            arco.getChildren().add(g);

            Label n = new Label(String.valueOf(d.fdi()));
            n.getStyleClass().add("visor-numero");
            n.setManaged(false);
            n.visibleProperty().bind(numerosVisibles.and(arco.visibleProperty()));
            numeros.put(d.fdi(), n);
            capaEtiquetas.getChildren().add(n);
        }
    }

    private void JE_JE_SR_JA_ME_construirEncias() {
        double[] c = modelo.JE_JE_SR_JA_ME_centro();
        PhongMaterial encia = new PhongMaterial(Color.web("#C9737D"));
        encia.setSpecularColor(Color.rgb(120, 90, 95));
        encia.setSpecularPower(22);
        for (JE_JE_SR_JA_ME_ModeloDental.JE_JE_SR_JA_ME_Encia e : modelo.JE_JE_SR_JA_ME_encias()) {
            MeshView mv = new MeshView(JE_JE_SR_JA_ME_malla(JE_JE_SR_JA_ME_aPuntos(e.malla(), c), JE_JE_SR_JA_ME_aNormales(e.normales()), e.triangulos()));
            mv.setMaterial(encia);
            mv.setCullFace(CullFace.BACK);
            mv.setMouseTransparent(true);
            mv.getStyleClass().add("encia");
            (e.superior() ? arcoSuperior : arcoInferior).getChildren().add(0, mv);
            mv.visibleProperty().bind(enciasVisibles);
        }
    }

    private final BooleanProperty enciasVisibles = new SimpleBooleanProperty(true);

    /** Pasa de coordenadas del OBJ (y arriba, z hacia el espectador) a JavaFX (y abajo). */
    private static float[] JE_JE_SR_JA_ME_aPuntos(JE_JE_SR_JA_ME_Malla m, double[] c) {
        float[] p = new float[m.JE_JE_SR_JA_ME_vertices() * 3];
        for (int i = 0; i < m.JE_JE_SR_JA_ME_vertices(); i++) {
            double[] v = m.JE_JE_SR_JA_ME_vertice(i);
            p[3 * i] = (float) ((v[0] - c[0]) * ESCALA);
            p[3 * i + 1] = (float) (-(v[1] - c[1]) * ESCALA);
            p[3 * i + 2] = (float) (-(v[2] - c[2]) * ESCALA);
        }
        return p;
    }

    private static float[] JE_JE_SR_JA_ME_aNormales(double[][] n) {
        float[] r = new float[n.length * 3];
        for (int i = 0; i < n.length; i++) {
            r[3 * i] = (float) n[i][0];
            r[3 * i + 1] = (float) -n[i][1];
            r[3 * i + 2] = (float) -n[i][2];
        }
        return r;
    }

    private static TriangleMesh JE_JE_SR_JA_ME_malla(float[] puntos, float[] normales, List<int[]> tris) {
        TriangleMesh m = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        m.getPoints().setAll(puntos);
        m.getNormals().setAll(normales);
        m.getTexCoords().setAll(0, 0);
        int[] caras = new int[tris.size() * 9];
        int k = 0;
        for (int[] t : tris) {
            for (int idx : t) {
                caras[k++] = idx;
                caras[k++] = idx;
                caras[k++] = 0;
            }
        }
        m.getFaces().setAll(caras);
        return m;
    }

    // ------------------------------------------------------------- materiales

    /**
     * Material por color y brillo. El brillo (resaltado de seleccion / hover) se logra
     * con un mapa de autoiluminacion de 1x1 pixel: todas las coordenadas de textura
     * apuntan a (0,0), asi que el diente entero "emite" ese color.
     */
    private PhongMaterial JE_JE_SR_JA_ME_material(Color color, Color brillo, boolean metalico) {
        String clave = color + "|" + brillo + "|" + metalico;
        return materiales.computeIfAbsent(clave, k -> {
            PhongMaterial m = new PhongMaterial(color);
            m.setSpecularColor(metalico ? Color.rgb(250, 250, 250) : Color.rgb(150, 148, 140));
            m.setSpecularPower(metalico ? 70 : 46);
            if (brillo != null) {
                WritableImage img = new WritableImage(1, 1);
                img.getPixelWriter().setColor(0, 0, brillo);
                m.setSelfIlluminationMap(img);
            }
            return m;
        });
    }

    private static final Color BRILLO_SELECCION = Color.rgb(28, 74, 140);
    private static final Color BRILLO_HOVER = Color.rgb(38, 46, 60);
    private static final Color BRILLO_CARA = Color.rgb(70, 110, 170);

    private void JE_JE_SR_JA_ME_repintar(int fdi) {
        JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_EstadoPieza estado = odontograma.JE_JE_SR_JA_ME_pieza(fdi);
        boolean ausente = estado.JE_JE_SR_JA_ME_ausente();
        boolean sel = seleccionada != null && seleccionada == fdi;
        boolean hover = hoverPieza != null && hoverPieza == fdi;
        caras.get(fdi).forEach((cara, mv) -> {
            JE_JE_SR_JA_ME_EstadoDental visible = estado.JE_JE_SR_JA_ME_visible(cara);
            Color color = Color.web(visible.JE_JE_SR_JA_ME_getColor());
            boolean metal = visible == JE_JE_SR_JA_ME_EstadoDental.IMPLANTE || visible == JE_JE_SR_JA_ME_EstadoDental.CORONA;
            Color brillo = null;
            if (hover && resaltarCara && cara == hoverCara && !ausente) {
                brillo = BRILLO_CARA;
            } else if (sel) {
                brillo = BRILLO_SELECCION;
            } else if (hover) {
                brillo = BRILLO_HOVER;
            }
            if (ausente) {
                mv.setDrawMode(DrawMode.LINE);
                mv.setMaterial(JE_JE_SR_JA_ME_material(sel ? Color.web("#7aa7e6") : Color.web("#9aa3b2"), null, false));
            } else {
                mv.setDrawMode(DrawMode.FILL);
                mv.setMaterial(JE_JE_SR_JA_ME_material(color, brillo, metal));
            }
        });
        Group g = nodosDiente.get(fdi);
        double escala = sel ? 1.07 : 1.0;
        g.setScaleX(escala);
        g.setScaleY(escala);
        g.setScaleZ(escala);
        Label n = numeros.get(fdi);
        n.getStyleClass().removeAll("seleccionado", "patologia");
        if (sel) {
            n.getStyleClass().add("seleccionado");
        } else if (JE_JE_SR_JA_ME_tienePatologia(estado)) {
            n.getStyleClass().add("patologia");
        }
    }

    private static boolean JE_JE_SR_JA_ME_tienePatologia(JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_EstadoPieza e) {
        if (e.JE_JE_SR_JA_ME_general() != null && e.JE_JE_SR_JA_ME_general().JE_JE_SR_JA_ME_esPatologia()) {
            return true;
        }
        return e.JE_JE_SR_JA_ME_caras().values().stream().anyMatch(JE_JE_SR_JA_ME_EstadoDental::JE_JE_SR_JA_ME_esPatologia);
    }

    private void JE_JE_SR_JA_ME_repintarTodo() {
        caras.keySet().forEach(this::JE_JE_SR_JA_ME_repintar);
    }

    // ------------------------------------------------------------------- API

    public void JE_JE_SR_JA_ME_setOdontograma(JE_JE_SR_JA_ME_Odontograma o) {
        this.odontograma = o == null ? JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_vacio() : o;
        JE_JE_SR_JA_ME_repintarTodo();
    }

    public void JE_JE_SR_JA_ME_seleccionar(Integer fdi) {
        Integer anterior = seleccionada;
        seleccionada = fdi;
        if (anterior != null) {
            JE_JE_SR_JA_ME_repintar(anterior);
        }
        if (fdi != null) {
            JE_JE_SR_JA_ME_repintar(fdi);
        }
    }

    /** Clic sobre una pieza: (fdi, cara pulsada). */
    public void JE_JE_SR_JA_ME_setAlPulsar(BiConsumer<Integer, JE_JE_SR_JA_ME_Superficie> accion) {
        this.alPulsar = accion;
    }

    /** Doble clic sobre una cara: (fdi, cara). Se usa para volverla sana. */
    public void JE_JE_SR_JA_ME_setAlDobleClic(BiConsumer<Integer, JE_JE_SR_JA_ME_Superficie> accion) {
        this.alDobleClic = accion;
    }

    /** Raton sobre una pieza: (fdi, cara) o (null, null) al salir. */
    public void JE_JE_SR_JA_ME_setAlPasar(BiConsumer<Integer, JE_JE_SR_JA_ME_Superficie> accion) {
        this.alPasar = accion;
    }

    /** Si es falso el hover resalta la pieza completa (herramientas de pieza). */
    public void JE_JE_SR_JA_ME_setResaltarCara(boolean resaltarCara) {
        this.resaltarCara = resaltarCara;
        if (hoverPieza != null) {
            JE_JE_SR_JA_ME_repintar(hoverPieza);
        }
    }

    public BooleanProperty JE_JE_SR_JA_ME_numerosVisiblesProperty() {
        return numerosVisibles;
    }

    public BooleanProperty JE_JE_SR_JA_ME_enciasVisiblesProperty() {
        return enciasVisibles;
    }

    public BooleanProperty JE_JE_SR_JA_ME_giroAutomaticoProperty() {
        return giroAutomatico;
    }

    public BooleanProperty JE_JE_SR_JA_ME_bocaAbiertaProperty() {
        return bocaAbierta;
    }

    // ------------------------------------------------------------------ vistas

    public void JE_JE_SR_JA_ME_vistaFrontal() {
        JE_JE_SR_JA_ME_animarVista(INCLINACION_INICIAL, 0, DISTANCIA_INICIAL);
    }

    /**
     * Arcada superior vista desde abajo: se oculta la inferior para dejar a la vista
     * todas las caras oclusales y palatinas.
     */
    public void JE_JE_SR_JA_ME_vistaSuperior() {
        JE_JE_SR_JA_ME_mostrarArcos(true, false);
        JE_JE_SR_JA_ME_animarVista(-80, 0, DISTANCIA_INICIAL * 0.95, false);
    }

    /** Arcada inferior vista desde arriba (se oculta la superior). */
    public void JE_JE_SR_JA_ME_vistaInferior() {
        JE_JE_SR_JA_ME_mostrarArcos(false, true);
        JE_JE_SR_JA_ME_animarVista(80, 0, DISTANCIA_INICIAL * 0.95, false);
    }

    private void JE_JE_SR_JA_ME_mostrarArcos(boolean superior, boolean inferior) {
        arcoSuperior.setVisible(superior);
        arcoInferior.setVisible(inferior);
    }

    public void JE_JE_SR_JA_ME_vistaDerecha() {
        JE_JE_SR_JA_ME_animarVista(-8, 70, DISTANCIA_INICIAL * 0.95);
    }

    public void JE_JE_SR_JA_ME_vistaIzquierda() {
        JE_JE_SR_JA_ME_animarVista(-8, -70, DISTANCIA_INICIAL * 0.95);
    }

    /** Centra suavemente la camara en una pieza para examinarla. */
    public void JE_JE_SR_JA_ME_enfocar(int fdi) {
        Point3D c = centros.get(fdi);
        if (c == null) {
            return;
        }
        // Gira de modo que la cara vestibular de la pieza mire a la camara.
        double angulo = Math.toDegrees(Math.atan2(c.getX(), -c.getZ() + 60));
        double inclinacion = JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_esSuperior(fdi) ? -10 : 22;
        JE_JE_SR_JA_ME_animarVista(inclinacion, -angulo, DISTANCIA_INICIAL * 0.62);
    }

    private void JE_JE_SR_JA_ME_animarVista(double rx, double ry, double distancia) {
        JE_JE_SR_JA_ME_animarVista(rx, ry, distancia, true);
    }

    private void JE_JE_SR_JA_ME_animarVista(double rx, double ry, double distancia, boolean ambosArcos) {
        if (ambosArcos) {
            JE_JE_SR_JA_ME_mostrarArcos(true, true);
        }
        if (animacionVista != null) {
            animacionVista.stop();
        }
        giroAutomatico.set(false);
        double destinoY = ry + Math.round((giroY.getAngle() - ry) / 360.0) * 360.0;
        Interpolator suave = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);
        animacionVista = new Timeline(new KeyFrame(Duration.millis(650),
                new KeyValue(giroX.angleProperty(), rx, suave),
                new KeyValue(giroY.angleProperty(), destinoY, suave),
                new KeyValue(camara.translateZProperty(), -distancia, suave),
                new KeyValue(desplazamiento.xProperty(), 0, suave),
                new KeyValue(desplazamiento.yProperty(), 0, suave)));
        animacionVista.play();
    }

    private void JE_JE_SR_JA_ME_animarApertura(boolean abrir) {
        Interpolator suave = Interpolator.SPLINE(0.3, 0.0, 0.2, 1);
        double distancia = abrir ? Math.max(-camara.getTranslateZ(), DISTANCIA_INICIAL * 1.3)
                : DISTANCIA_INICIAL;
        new Timeline(new KeyFrame(Duration.millis(700),
                new KeyValue(camara.translateZProperty(), -distancia, suave),
                new KeyValue(aperturaSup.angleProperty(), abrir ? -16 : 0, suave),
                new KeyValue(aperturaInf.angleProperty(), abrir ? 16 : 0, suave),
                new KeyValue(separacionSup.yProperty(), abrir ? -38 : 0, suave),
                new KeyValue(separacionInf.yProperty(), abrir ? 38 : 0, suave))).play();
    }

    // ------------------------------------------------------------------ raton

    private void JE_JE_SR_JA_ME_instalarRaton() {
        subEscena.setOnMousePressed(e -> {
            ratonX = inicioX = e.getSceneX();
            ratonY = inicioY = e.getSceneY();
            arrastrando = false;
        });
        subEscena.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - ratonX;
            double dy = e.getSceneY() - ratonY;
            ratonX = e.getSceneX();
            ratonY = e.getSceneY();
            if (Math.abs(e.getSceneX() - inicioX) + Math.abs(e.getSceneY() - inicioY) > 4) {
                arrastrando = true;
                giroAutomatico.set(false);
                if (animacionVista != null) {
                    animacionVista.stop();
                }
            }
            if (!arrastrando) {
                return;
            }
            if (e.getButton() == MouseButton.SECONDARY || e.isShiftDown()) {
                desplazamiento.setX(desplazamiento.getX() + dx);
                desplazamiento.setY(desplazamiento.getY() + dy);
            } else {
                giroY.setAngle(giroY.getAngle() - dx * 0.45);
                giroX.setAngle(Math.max(-89, Math.min(89, giroX.getAngle() + dy * 0.45)));
            }
        });
        subEscena.setOnScroll(e -> {
            double z = camara.getTranslateZ() + e.getDeltaY() * 1.1;
            camara.setTranslateZ(Math.max(-2200, Math.min(-380, z)));
        });
        subEscena.setOnMouseClicked(e -> {
            if (arrastrando || e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            Object[] info = JE_JE_SR_JA_ME_info(e.getPickResult());
            if (info != null && e.getClickCount() == 2) {
                alDobleClic.accept((Integer) info[0], (JE_JE_SR_JA_ME_Superficie) info[1]);
            } else if (info != null) {
                alPulsar.accept((Integer) info[0], (JE_JE_SR_JA_ME_Superficie) info[1]);
            } else if (e.getClickCount() == 2) {
                JE_JE_SR_JA_ME_vistaFrontal();
            }
        });
        subEscena.setOnMouseMoved(this::JE_JE_SR_JA_ME_actualizarHover);
        subEscena.setOnMouseExited(e -> JE_JE_SR_JA_ME_limpiarHover());
    }

    private static Object[] JE_JE_SR_JA_ME_info(PickResult pr) {
        Node n = pr == null ? null : pr.getIntersectedNode();
        if (n instanceof MeshView mv && mv.getUserData() instanceof Object[] datos) {
            return datos;
        }
        return null;
    }

    private void JE_JE_SR_JA_ME_actualizarHover(MouseEvent e) {
        Object[] info = JE_JE_SR_JA_ME_info(e.getPickResult());
        if (info == null) {
            JE_JE_SR_JA_ME_limpiarHover();
            return;
        }
        int fdi = (Integer) info[0];
        JE_JE_SR_JA_ME_Superficie cara = (JE_JE_SR_JA_ME_Superficie) info[1];
        Integer anterior = hoverPieza;
        JE_JE_SR_JA_ME_Superficie caraAnterior = hoverCara;
        hoverPieza = fdi;
        hoverCara = cara;
        if (anterior != null && anterior != fdi) {
            JE_JE_SR_JA_ME_repintar(anterior);
        }
        if (anterior == null || anterior != fdi || caraAnterior != cara) {
            JE_JE_SR_JA_ME_repintar(fdi);
            alPasar.accept(fdi, cara);
        }
        setCursor(javafx.scene.Cursor.HAND);

        JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_EstadoPieza estado = odontograma.JE_JE_SR_JA_ME_pieza(fdi);
        String texto = fdi + " · " + JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_nombre(fdi);
        if (!estado.JE_JE_SR_JA_ME_ausente()) {
            texto += "\n" + cara.JE_JE_SR_JA_ME_etiqueta(fdi) + " — " + estado.JE_JE_SR_JA_ME_visible(cara).JE_JE_SR_JA_ME_getEtiqueta();
        } else {
            texto += "\nPieza ausente";
        }
        etiquetaHover.setText(texto);
        etiquetaHover.autosize();
        Point2D p = capaEtiquetas.sceneToLocal(e.getSceneX(), e.getSceneY());
        double x = Math.min(p.getX() + 16, getWidth() - etiquetaHover.getWidth() - 8);
        double y = Math.min(p.getY() + 18, getHeight() - etiquetaHover.getHeight() - 8);
        etiquetaHover.relocate(x, y);
        etiquetaHover.setVisible(true);
    }

    private void JE_JE_SR_JA_ME_limpiarHover() {
        Integer anterior = hoverPieza;
        hoverPieza = null;
        hoverCara = null;
        etiquetaHover.setVisible(false);
        setCursor(javafx.scene.Cursor.DEFAULT);
        if (anterior != null) {
            JE_JE_SR_JA_ME_repintar(anterior);
            alPasar.accept(null, null);
        }
    }

    // ------------------------------------------------- giro y numeros flotantes

    private void JE_JE_SR_JA_ME_instalarBucle() {
        bocaAbierta.addListener((o, a, abrir) -> JE_JE_SR_JA_ME_animarApertura(abrir));
        AnimationTimer bucle = new AnimationTimer() {
            private long anterior;

            @Override
            public void handle(long ahora) {
                if (giroAutomatico.get() && anterior > 0) {
                    giroY.setAngle(giroY.getAngle() + Math.min(0.05, (ahora - anterior) / 1e9) * 14);
                }
                anterior = ahora;
                if (numerosVisibles.get()) {
                    JE_JE_SR_JA_ME_ubicarNumeros();
                }
            }
        };
        // El bucle solo corre mientras el visor esta en una ventana visible: al cambiar
        // de escena (cerrar sesion, cambiar de paciente) se detiene y no consume CPU.
        javafx.beans.value.ChangeListener<javafx.stage.Window> alVentana = (o, a, w) -> {
            if (w == null) {
                bucle.stop();
            } else {
                bucle.start();
            }
        };
        sceneProperty().addListener((o, anterior, escena) -> {
            if (anterior != null) {
                anterior.windowProperty().removeListener(alVentana);
            }
            if (escena == null) {
                bucle.stop();
                return;
            }
            escena.windowProperty().addListener(alVentana);
            if (escena.getWindow() != null) {
                bucle.start();
            } else {
                bucle.stop();
            }
        });
    }

    /** Proyecta el centro de cada diente a la pantalla y coloca su numero encima. */
    private void JE_JE_SR_JA_ME_ubicarNumeros() {
        for (Map.Entry<Integer, Group> e : nodosDiente.entrySet()) {
            int fdi = e.getKey();
            Point3D c = centros.get(fdi);
            // Un poco hacia afuera de la encia para que el numero no tape el diente.
            double fuera = JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_esSuperior(fdi) ? -2.6 * ESCALA : 2.6 * ESCALA;
            Group arco = JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_esSuperior(fdi) ? arcoSuperior : arcoInferior;
            Point3D local = new Point3D(c.getX() * 1.12, c.getY() + fuera, c.getZ() * 1.08 - 10);
            // rootScene = true: proyecta a traves de la camara de la SubScene.
            Point3D proyectado = arco.localToScene(local, true);
            Label n = numeros.get(fdi);
            Point2D enCapa = capaEtiquetas.sceneToLocal(proyectado.getX(), proyectado.getY());
            if (enCapa == null) {
                continue;
            }
            n.autosize();
            n.relocate(enCapa.getX() - n.getWidth() / 2, enCapa.getY() - n.getHeight() / 2);
            // Los numeros de piezas de espaldas a la camara se atenuan.
            Point3D enMundo = arco.localToScene(local);
            n.setOpacity(enMundo.getZ() > 40 ? 0.35 : 1.0);
        }
    }

    // ------------------------------------------------------------- captura

    /** Vistas predefinidas para capturas sin animación (PDF). */
    public enum JE_JE_SR_JA_ME_Vista { FRONTAL, SUPERIOR, INFERIOR }

    /** Coloca la cámara al instante (sin animación). */
    public void JE_JE_SR_JA_ME_ponerVista(JE_JE_SR_JA_ME_Vista vista) {
        if (animacionVista != null) {
            animacionVista.stop();
        }
        giroAutomatico.set(false);
        desplazamiento.setX(0);
        desplazamiento.setY(0);
        giroY.setAngle(0);
        switch (vista) {
            case FRONTAL -> {
                JE_JE_SR_JA_ME_mostrarArcos(true, true);
                giroX.setAngle(INCLINACION_INICIAL);
                camara.setTranslateZ(-DISTANCIA_INICIAL * 0.92);
            }
            case SUPERIOR -> {
                JE_JE_SR_JA_ME_mostrarArcos(true, false);
                giroX.setAngle(-80);
                camara.setTranslateZ(-DISTANCIA_INICIAL * 0.95);
            }
            case INFERIOR -> {
                JE_JE_SR_JA_ME_mostrarArcos(false, true);
                giroX.setAngle(80);
                camara.setTranslateZ(-DISTANCIA_INICIAL * 0.95);
            }
        }
    }

}
