package com.dentavista.servicio;

import com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Odontograma;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Pieza;
import com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoItem;
import com.dentavista.modelo.JE_JE_SR_JA_ME_ItemPlan;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Pago;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Receta;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Usuario;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Documentos en PDF, cada uno con un propósito distinto (sin repetir contenido):
 * <ul>
 *   <li><b>Informe clínico</b>: odontograma 3D y 2D, hallazgos y consultas. Sin importes.</li>
 *   <li><b>Presupuesto</b>: tratamientos con importes, lo pagado y el saldo. Sin imágenes.</li>
 *   <li><b>Recibo</b> (A5) y <b>receta</b> (A5).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class JE_JE_SR_JA_ME_PdfService {

    /** Imágenes PNG ya renderizadas del odontograma. */
    public record JE_JE_SR_JA_ME_Imagenes(byte[] frontal, byte[] superior, byte[] inferior, byte[] odontograma2d) {
    }

    private static final Color TURQUESA = new Color(13, 148, 136);
    private static final Color TINTA = new Color(15, 23, 42);
    private static final Color GRIS = new Color(100, 116, 139);
    private static final Color LINEA = new Color(226, 232, 240);
    private static final Color FONDO = new Color(248, 250, 252);
    private static final Color ROJO = new Color(185, 28, 28);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JE_JE_SR_JA_ME_DatosClinica clinica;
    private final JE_JE_SR_JA_ME_SesionService sesion;

    private Font JE_JE_SR_JA_ME_f(float tam, int estilo, Color c) {
        return new Font(Font.HELVETICA, tam, estilo, c);
    }

    /** Escribe un documento en el flujo de salida. */
    @FunctionalInterface
    interface JE_JE_SR_JA_ME_Escritor {
        void JE_JE_SR_JA_ME_escribir(OutputStream out) throws DocumentException, IOException;
    }

    /** Informe clínico: odontograma, hallazgos y consultas (sin importes). */
    public Path JE_JE_SR_JA_ME_informe(JE_JE_SR_JA_ME_Paciente p, JE_JE_SR_JA_ME_Odontograma o, List<com.dentavista.modelo.JE_JE_SR_JA_ME_Consulta> consultas, JE_JE_SR_JA_ME_Imagenes img) {
        return JE_JE_SR_JA_ME_guardar("Informe", p, out -> JE_JE_SR_JA_ME_escribirInforme(out, p, o, consultas, img));
    }

    /** Presupuesto: tratamientos con importes, lo pagado y el saldo por pagar. */
    public Path JE_JE_SR_JA_ME_presupuesto(JE_JE_SR_JA_ME_Paciente p, List<JE_JE_SR_JA_ME_ItemPlan> items, JE_JE_SR_JA_ME_PlanService.JE_JE_SR_JA_ME_Totales totales, JE_JE_SR_JA_ME_PagoService.JE_JE_SR_JA_ME_Cuenta cuenta) {
        return JE_JE_SR_JA_ME_guardar("Presupuesto", p, out -> JE_JE_SR_JA_ME_escribirPresupuesto(out, p, items, totales, cuenta));
    }

    private Path JE_JE_SR_JA_ME_guardar(String tipo, JE_JE_SR_JA_ME_Paciente p, JE_JE_SR_JA_ME_Escritor escritor) {
        try {
            String nombre = tipo + "_" + p.getApellidos().split(" ")[0] + "_" + p.getDni() + "_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf";
            Path destino = JE_JE_SR_JA_ME_carpetaSalida().resolve(nombre);
            try (OutputStream out = Files.newOutputStream(destino)) {
                escritor.JE_JE_SR_JA_ME_escribir(out);
            }
            return destino;
        } catch (IOException | DocumentException e) {
            throw new JE_JE_SR_JA_ME_NegocioException("No se pudo generar el PDF: " + e.getMessage());
        }
    }

    /** Documentos\DentaVista (o la carpeta personal si no existe Documentos). */
    public static Path JE_JE_SR_JA_ME_carpetaSalida() throws IOException {
        Path home = Path.of(System.getProperty("user.home"));
        Path docs = home.resolve("Documents");
        Path carpeta = (Files.isDirectory(docs) ? docs : home).resolve("DentaVista");
        Files.createDirectories(carpeta);
        return carpeta;
    }

    private Document JE_JE_SR_JA_ME_abrirA4(OutputStream out, String titulo, JE_JE_SR_JA_ME_Paciente p) throws DocumentException {
        Document doc = new Document(PageSize.A4, 36, 36, 34, 46);
        PdfWriter w = PdfWriter.getInstance(doc, out);
        w.setPageEvent(new JE_JE_SR_JA_ME_Pie());
        doc.addTitle(titulo + " - " + p.JE_JE_SR_JA_ME_nombreCompleto());
        doc.addCreator("DentaVista");
        doc.open();
        JE_JE_SR_JA_ME_cabecera(doc, titulo.toUpperCase(), "Emitido el " + LocalDateTime.now().format(FECHA_HORA), 15);
        JE_JE_SR_JA_ME_datosPaciente(doc, p);
        return doc;
    }

    void JE_JE_SR_JA_ME_escribirInforme(OutputStream out, JE_JE_SR_JA_ME_Paciente p, JE_JE_SR_JA_ME_Odontograma o, List<com.dentavista.modelo.JE_JE_SR_JA_ME_Consulta> consultas,
                         JE_JE_SR_JA_ME_Imagenes img) throws DocumentException, IOException {
        Document doc = JE_JE_SR_JA_ME_abrirA4(out, "Informe clínico", p);

        JE_JE_SR_JA_ME_titulo(doc, "Odontograma 3D");
        PdfPTable vistas = new PdfPTable(new float[]{1.45f, 1f});
        vistas.setWidthPercentage(100);
        PdfPCell grande = JE_JE_SR_JA_ME_celdaImagen(img.frontal(), "Vista frontal");
        grande.setRowspan(2);
        vistas.addCell(grande);
        vistas.addCell(JE_JE_SR_JA_ME_celdaImagen(img.superior(), "Arcada superior (oclusal)"));
        vistas.addCell(JE_JE_SR_JA_ME_celdaImagen(img.inferior(), "Arcada inferior (oclusal)"));
        doc.add(vistas);

        JE_JE_SR_JA_ME_titulo(doc, "Odontograma clínico (notación FDI)");
        Image i2d = Image.getInstance(img.odontograma2d());
        i2d.scaleToFit(PageSize.A4.getWidth() - 72, 190);
        i2d.setAlignment(Element.ALIGN_CENTER);
        doc.add(i2d);
        JE_JE_SR_JA_ME_leyenda(doc);

        // Las imágenes llenan la primera hoja: el detalle clínico empieza en una nueva.
        doc.newPage();
        JE_JE_SR_JA_ME_titulo(doc, "Hallazgos actuales");
        JE_JE_SR_JA_ME_hallazgos(doc, o);

        JE_JE_SR_JA_ME_titulo(doc, "Consultas");
        JE_JE_SR_JA_ME_consultas(doc, consultas);

        JE_JE_SR_JA_ME_firmas(doc, "Recibí el informe");
        doc.close();
    }

    void JE_JE_SR_JA_ME_escribirPresupuesto(OutputStream out, JE_JE_SR_JA_ME_Paciente p, List<JE_JE_SR_JA_ME_ItemPlan> items, JE_JE_SR_JA_ME_PlanService.JE_JE_SR_JA_ME_Totales totales,
                             JE_JE_SR_JA_ME_PagoService.JE_JE_SR_JA_ME_Cuenta cuenta) throws DocumentException {
        Document doc = JE_JE_SR_JA_ME_abrirA4(out, "Presupuesto", p);

        JE_JE_SR_JA_ME_titulo(doc, "Plan de tratamiento");
        JE_JE_SR_JA_ME_plan(doc, items, totales, cuenta);

        Paragraph validez = new Paragraph("Presupuesto válido por 30 días. Los importes están expresados en soles (S/) "
                + "e incluyen IGV. El plan puede ajustarse según la evolución clínica.", JE_JE_SR_JA_ME_f(8.5f, Font.ITALIC, GRIS));
        validez.setSpacingBefore(8);
        doc.add(validez);

        JE_JE_SR_JA_ME_firmas(doc, "Acepto el plan y presupuesto");
        doc.close();
    }

    // ------------------------------------------------------------- recibo

    /** Recibo de pago (A5) con el estado de cuenta después del abono. */
    public Path JE_JE_SR_JA_ME_recibo(JE_JE_SR_JA_ME_Pago pago, JE_JE_SR_JA_ME_PagoService.JE_JE_SR_JA_ME_Cuenta cuenta) {
        try {
            Path destino = JE_JE_SR_JA_ME_carpetaSalida().resolve("Recibo_" + pago.getRecibo() + ".pdf");
            try (OutputStream out = Files.newOutputStream(destino)) {
                Document doc = new Document(PageSize.A5, 30, 30, 28, 40);
                PdfWriter w = PdfWriter.getInstance(doc, out);
                w.setPageEvent(new JE_JE_SR_JA_ME_Pie());
                doc.open();
                JE_JE_SR_JA_ME_cabecera(doc, "RECIBO DE PAGO\n" + pago.getRecibo(), pago.getFecha().format(FECHA_HORA), 12);
                JE_JE_SR_JA_ME_Paciente p = pago.getPaciente();
                PdfPTable d = new PdfPTable(2);
                d.setWidthPercentage(100);
                d.setSpacingBefore(10);
                JE_JE_SR_JA_ME_dato(d, "PACIENTE", p.JE_JE_SR_JA_ME_nombreCompleto());
                JE_JE_SR_JA_ME_dato(d, "DNI", p.getDni());
                doc.add(d);

                PdfPTable t = new PdfPTable(new float[]{2.4f, 1f});
                t.setWidthPercentage(100);
                t.setSpacingBefore(12);
                PdfPCell c1 = new PdfPCell(new Phrase("Abono a plan de tratamiento odontológico"
                        + (pago.getNota() != null ? "\n" + pago.getNota() : "") + "\nMedio de pago: " + pago.getMedio().JE_JE_SR_JA_ME_getEtiqueta(),
                        JE_JE_SR_JA_ME_f(9.5f, Font.NORMAL, TINTA)));
                c1.setPadding(8);
                c1.setBorderColor(LINEA);
                PdfPCell c2 = new PdfPCell(new Phrase(JE_JE_SR_JA_ME_soles(pago.getMonto()), JE_JE_SR_JA_ME_f(14, Font.BOLD, TURQUESA)));
                c2.setPadding(8);
                c2.setBorderColor(LINEA);
                c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
                c2.setVerticalAlignment(Element.ALIGN_MIDDLE);
                t.addCell(c1);
                t.addCell(c2);
                doc.add(t);

                PdfPTable cta = new PdfPTable(new float[]{2f, 1f});
                cta.setWidthPercentage(70);
                cta.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cta.setSpacingBefore(10);
                JE_JE_SR_JA_ME_filaCuenta(cta, "Total del plan", cuenta.totalPlan(), false);
                JE_JE_SR_JA_ME_filaCuenta(cta, "Tratamientos realizados", cuenta.realizado(), false);
                JE_JE_SR_JA_ME_filaCuenta(cta, "Total pagado a la fecha", cuenta.pagado(), false);
                if (cuenta.JE_JE_SR_JA_ME_deuda().signum() > 0) {
                    JE_JE_SR_JA_ME_filaCuenta(cta, "Debe por tratamientos realizados", cuenta.JE_JE_SR_JA_ME_deuda(), false);
                } else if (cuenta.JE_JE_SR_JA_ME_aFavor().signum() > 0) {
                    JE_JE_SR_JA_ME_filaCuenta(cta, "Adelanto (pagado, aún no realizado)", cuenta.JE_JE_SR_JA_ME_aFavor(), false);
                }
                JE_JE_SR_JA_ME_filaCuenta(cta, "Saldo por pagar del plan", cuenta.JE_JE_SR_JA_ME_porPagarDelPlan(), true);
                doc.add(cta);
                if (pago.isAnulado()) {
                    Paragraph an = new Paragraph("RECIBO ANULADO: " + pago.getMotivoAnulacion(), JE_JE_SR_JA_ME_f(11, Font.BOLD, ROJO));
                    an.setSpacingBefore(10);
                    doc.add(an);
                }
                PdfPTable fir = new PdfPTable(1);
                fir.setWidthPercentage(55);
                fir.setSpacingBefore(40);
                fir.addCell(JE_JE_SR_JA_ME_firma(pago.getRegistradoPor() == null ? "Caja" : pago.getRegistradoPor(), "Recibido por"));
                doc.add(fir);
                doc.close();
            }
            return destino;
        } catch (IOException | DocumentException e) {
            throw new JE_JE_SR_JA_ME_NegocioException("No se pudo generar el recibo: " + e.getMessage());
        }
    }

    private void JE_JE_SR_JA_ME_filaCuenta(PdfPTable t, String rotulo, BigDecimal monto, boolean destacado) {
        PdfPCell a = new PdfPCell(new Phrase(rotulo, JE_JE_SR_JA_ME_f(destacado ? 10 : 9, Font.BOLD, destacado ? TURQUESA : GRIS)));
        a.setBorder(destacado ? Rectangle.TOP : Rectangle.NO_BORDER);
        a.setBorderColor(TURQUESA);
        a.setPadding(4);
        PdfPCell b = new PdfPCell(new Phrase(JE_JE_SR_JA_ME_soles(monto), JE_JE_SR_JA_ME_f(destacado ? 11 : 9.5f, Font.BOLD, destacado ? TURQUESA : TINTA)));
        b.setBorder(destacado ? Rectangle.TOP : Rectangle.NO_BORDER);
        b.setBorderColor(TURQUESA);
        b.setHorizontalAlignment(Element.ALIGN_RIGHT);
        b.setPadding(4);
        t.addCell(a);
        t.addCell(b);
    }

    // ------------------------------------------------------------- receta

    /** Receta médica (A5) con alergias destacadas y firma del odontólogo. */
    public Path JE_JE_SR_JA_ME_receta(JE_JE_SR_JA_ME_Receta r) {
        try {
            Path destino = JE_JE_SR_JA_ME_carpetaSalida().resolve("Receta_" + r.getPaciente().getDni() + "_"
                    + r.getFecha().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
            try (OutputStream out = Files.newOutputStream(destino)) {
                Document doc = new Document(PageSize.A5, 30, 30, 28, 40);
                PdfWriter w = PdfWriter.getInstance(doc, out);
                w.setPageEvent(new JE_JE_SR_JA_ME_Pie());
                doc.open();
                JE_JE_SR_JA_ME_cabecera(doc, "RECETA", r.getFecha().format(FECHA_HORA), 12);
                JE_JE_SR_JA_ME_Paciente p = r.getPaciente();
                PdfPTable d = new PdfPTable(new float[]{2.2f, 1f, 0.8f});
                d.setWidthPercentage(100);
                d.setSpacingBefore(10);
                JE_JE_SR_JA_ME_dato(d, "PACIENTE", p.JE_JE_SR_JA_ME_nombreCompleto());
                JE_JE_SR_JA_ME_dato(d, "DNI", p.getDni());
                JE_JE_SR_JA_ME_dato(d, "EDAD", p.JE_JE_SR_JA_ME_edad() == null ? "—" : p.JE_JE_SR_JA_ME_edad() + " años");
                doc.add(d);
                if (p.getAlergias() != null) {
                    Paragraph al = new Paragraph("ALERGIAS: " + p.getAlergias(), JE_JE_SR_JA_ME_f(9, Font.BOLD, ROJO));
                    al.setSpacingBefore(6);
                    doc.add(al);
                }
                Paragraph rp = new Paragraph("Rp.", JE_JE_SR_JA_ME_f(16, Font.BOLDITALIC, TURQUESA));
                rp.setSpacingBefore(12);
                doc.add(rp);
                int n = 1;
                for (JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item i : r.getItems()) {
                    Paragraph m = new Paragraph(n++ + ".  " + i.getMedicamento(), JE_JE_SR_JA_ME_f(10.5f, Font.BOLD, TINTA));
                    m.setSpacingBefore(6);
                    doc.add(m);
                    doc.add(new Paragraph("     " + i.getIndicacion(), JE_JE_SR_JA_ME_f(9.5f, Font.NORMAL, TINTA)));
                }
                if (r.getIndicaciones() != null) {
                    Paragraph t = new Paragraph("Indicaciones: " + r.getIndicaciones(), JE_JE_SR_JA_ME_f(9, Font.ITALIC, GRIS));
                    t.setSpacingBefore(12);
                    doc.add(t);
                }
                JE_JE_SR_JA_ME_Usuario u = r.getOdontologo();
                PdfPTable fir = new PdfPTable(1);
                fir.setWidthPercentage(60);
                fir.setSpacingBefore(46);
                fir.addCell(JE_JE_SR_JA_ME_firma(u.getNombre(), u.getColegiatura() == null ? "Cirujano dentista"
                        : "Cirujano dentista · " + u.getColegiatura()));
                doc.add(fir);
                doc.close();
            }
            return destino;
        } catch (IOException | DocumentException e) {
            throw new JE_JE_SR_JA_ME_NegocioException("No se pudo generar la receta: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- bloques

    private void JE_JE_SR_JA_ME_cabecera(Document doc, String titulo, String detalle, float tamNombre) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{1.3f, 1f});
        t.setWidthPercentage(100);
        PdfPCell izq = new PdfPCell();
        izq.setBorder(Rectangle.NO_BORDER);
        izq.addElement(new Paragraph(clinica.nombre(), JE_JE_SR_JA_ME_f(tamNombre, Font.BOLD, TURQUESA)));
        izq.addElement(new Paragraph("RUC " + clinica.ruc() + " · " + clinica.direccion(), JE_JE_SR_JA_ME_f(8.5f, Font.NORMAL, GRIS)));
        izq.addElement(new Paragraph(clinica.telefono() + " · " + clinica.email(), JE_JE_SR_JA_ME_f(8.5f, Font.NORMAL, GRIS)));
        t.addCell(izq);

        PdfPCell der = new PdfPCell();
        der.setBorder(Rectangle.NO_BORDER);
        der.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph tit = new Paragraph(titulo, JE_JE_SR_JA_ME_f(12, Font.BOLD, TINTA));
        tit.setAlignment(Element.ALIGN_RIGHT);
        der.addElement(tit);
        Paragraph fecha = new Paragraph(detalle, JE_JE_SR_JA_ME_f(8.5f, Font.NORMAL, GRIS));
        fecha.setAlignment(Element.ALIGN_RIGHT);
        der.addElement(fecha);
        t.addCell(der);
        doc.add(t);

        PdfPTable linea = new PdfPTable(1);
        linea.setWidthPercentage(100);
        linea.setSpacingBefore(6);
        PdfPCell c = new PdfPCell();
        c.setFixedHeight(3);
        c.setBorder(Rectangle.NO_BORDER);
        c.setBackgroundColor(TURQUESA);
        linea.addCell(c);
        doc.add(linea);
    }

    private void JE_JE_SR_JA_ME_datosPaciente(Document doc, JE_JE_SR_JA_ME_Paciente p) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{1, 1, 1, 1});
        t.setWidthPercentage(100);
        t.setSpacingBefore(10);
        JE_JE_SR_JA_ME_dato(t, "PACIENTE", p.JE_JE_SR_JA_ME_nombreCompleto());
        JE_JE_SR_JA_ME_dato(t, "DNI", p.getDni());
        JE_JE_SR_JA_ME_dato(t, "EDAD", p.JE_JE_SR_JA_ME_edad() == null ? "—" : p.JE_JE_SR_JA_ME_edad() + " años");
        JE_JE_SR_JA_ME_dato(t, "TELÉFONO", p.getTelefono() == null ? "—" : p.getTelefono());
        doc.add(t);
        if (p.getAlergias() != null || p.getAntecedentes() != null) {
            PdfPTable a = new PdfPTable(1);
            a.setWidthPercentage(100);
            a.setSpacingBefore(4);
            Phrase ph = new Phrase();
            if (p.getAlergias() != null) {
                ph.add(new Chunk("ALERGIA: ", JE_JE_SR_JA_ME_f(9, Font.BOLD, ROJO)));
                ph.add(new Chunk(p.getAlergias() + "    ", JE_JE_SR_JA_ME_f(9, Font.NORMAL, ROJO)));
            }
            if (p.getAntecedentes() != null) {
                ph.add(new Chunk("ANTECEDENTES: ", JE_JE_SR_JA_ME_f(9, Font.BOLD, new Color(180, 83, 9))));
                ph.add(new Chunk(p.getAntecedentes(), JE_JE_SR_JA_ME_f(9, Font.NORMAL, new Color(180, 83, 9))));
            }
            PdfPCell c = new PdfPCell(ph);
            c.setBackgroundColor(new Color(254, 242, 242));
            c.setBorderColor(new Color(254, 202, 202));
            c.setPadding(7);
            a.addCell(c);
            doc.add(a);
        }
    }

    private void JE_JE_SR_JA_ME_dato(PdfPTable t, String rotulo, String valor) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setBackgroundColor(FONDO);
        c.setPadding(7);
        c.addElement(new Paragraph(rotulo, JE_JE_SR_JA_ME_f(7.5f, Font.BOLD, GRIS)));
        c.addElement(new Paragraph(valor, JE_JE_SR_JA_ME_f(10, Font.BOLD, TINTA)));
        t.addCell(c);
    }

    private void JE_JE_SR_JA_ME_titulo(Document doc, String texto) throws DocumentException {
        Paragraph p = new Paragraph(texto.toUpperCase(), JE_JE_SR_JA_ME_f(9.5f, Font.BOLD, TURQUESA));
        p.setSpacingBefore(12);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private PdfPCell JE_JE_SR_JA_ME_celdaImagen(byte[] png, String pie) throws IOException, DocumentException {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(3);
        Image im = Image.getInstance(png);
        im.setWidthPercentage(100);
        c.addElement(im);
        Paragraph l = new Paragraph(pie, JE_JE_SR_JA_ME_f(7.5f, Font.NORMAL, GRIS));
        l.setAlignment(Element.ALIGN_CENTER);
        c.addElement(l);
        return c;
    }

    private void JE_JE_SR_JA_ME_leyenda(Document doc) throws DocumentException {
        Phrase ph = new Phrase();
        for (JE_JE_SR_JA_ME_EstadoDental e : JE_JE_SR_JA_ME_EstadoDental.values()) {
            if (e == JE_JE_SR_JA_ME_EstadoDental.SANO) {
                continue;
            }
            java.awt.Color c = java.awt.Color.decode(e.JE_JE_SR_JA_ME_getColor());
            ph.add(new Chunk("■ ", JE_JE_SR_JA_ME_f(10, Font.BOLD, c)));
            ph.add(new Chunk(e.JE_JE_SR_JA_ME_getEtiqueta() + "     ", JE_JE_SR_JA_ME_f(7.5f, Font.NORMAL, GRIS)));
        }
        Paragraph p = new Paragraph(ph);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
    }

    private void JE_JE_SR_JA_ME_hallazgos(Document doc, JE_JE_SR_JA_ME_Odontograma o) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{0.5f, 2.2f, 3.3f});
        t.setWidthPercentage(100);
        JE_JE_SR_JA_ME_encabezado(t, "Pieza", "Descripción", "Estado");
        int filas = 0;
        for (JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_EstadoPieza ep : o.JE_JE_SR_JA_ME_piezas()) {
            if (ep.JE_JE_SR_JA_ME_sana()) {
                continue;
            }
            JE_JE_SR_JA_ME_fila(t, filas++ % 2 == 1, String.valueOf(ep.JE_JE_SR_JA_ME_fdi()), JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_nombre(ep.JE_JE_SR_JA_ME_fdi()), ep.JE_JE_SR_JA_ME_descripcion());
        }
        if (filas == 0) {
            PdfPCell c = new PdfPCell(new Phrase("Sin hallazgos: todas las piezas presentes están sanas.", JE_JE_SR_JA_ME_f(9, Font.ITALIC, GRIS)));
            c.setColspan(3);
            c.setPadding(8);
            c.setBorderColor(LINEA);
            t.addCell(c);
        }
        doc.add(t);
    }

    private void JE_JE_SR_JA_ME_consultas(Document doc, List<com.dentavista.modelo.JE_JE_SR_JA_ME_Consulta> lista) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{0.9f, 1.4f, 3.7f});
        t.setWidthPercentage(100);
        JE_JE_SR_JA_ME_encabezado(t, "Fecha", "Odontólogo", "Motivo y notas");
        int n = 0;
        for (com.dentavista.modelo.JE_JE_SR_JA_ME_Consulta c : lista) {
            JE_JE_SR_JA_ME_fila(t, n++ % 2 == 1, c.getFecha().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    c.getOdontologo() == null ? "—" : c.getOdontologo().getNombre(),
                    c.getMotivo() + (c.getNotas() != null && !c.getNotas().isBlank() ? "\n" + c.getNotas() : ""));
        }
        if (n == 0) {
            PdfPCell c = new PdfPCell(new Phrase("Aún no hay consultas registradas.", JE_JE_SR_JA_ME_f(9, Font.ITALIC, GRIS)));
            c.setColspan(3);
            c.setPadding(8);
            c.setBorderColor(LINEA);
            t.addCell(c);
        }
        doc.add(t);
    }

    private void JE_JE_SR_JA_ME_plan(Document doc, List<JE_JE_SR_JA_ME_ItemPlan> items, JE_JE_SR_JA_ME_PlanService.JE_JE_SR_JA_ME_Totales totales,
                      JE_JE_SR_JA_ME_PagoService.JE_JE_SR_JA_ME_Cuenta cuenta) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{0.35f, 1.1f, 2.6f, 0.95f, 0.9f});
        t.setWidthPercentage(100);
        JE_JE_SR_JA_ME_encabezado(t, "#", "Pieza / cara", "Tratamiento", "Estado", "Importe");
        int n = 0;
        for (JE_JE_SR_JA_ME_ItemPlan i : items) {
            if (i.getEstado() == JE_JE_SR_JA_ME_EstadoItem.CANCELADO) {
                continue;
            }
            n++;
            JE_JE_SR_JA_ME_fila(t, n % 2 == 0, String.valueOf(n), i.JE_JE_SR_JA_ME_ubicacion(), i.getTratamiento().getNombre()
                    + (i.getNota() != null ? "\n" + i.getNota() : ""), i.getEstado().JE_JE_SR_JA_ME_getEtiqueta(), JE_JE_SR_JA_ME_soles(i.getPrecio()));
        }
        if (n == 0) {
            PdfPCell c = new PdfPCell(new Phrase("No hay tratamientos en el plan.", JE_JE_SR_JA_ME_f(9, Font.ITALIC, GRIS)));
            c.setColspan(5);
            c.setPadding(8);
            c.setBorderColor(LINEA);
            t.addCell(c);
        }
        doc.add(t);
        // Totales en tabla aparte para que nunca queden solos bajo una cabecera repetida.
        PdfPTable tot = new PdfPTable(new float[]{0.35f, 1.1f, 2.6f, 0.95f, 0.9f});
        tot.setWidthPercentage(100);
        tot.setKeepTogether(true);
        JE_JE_SR_JA_ME_total(tot, "Realizado", totales.realizado(), false);
        JE_JE_SR_JA_ME_total(tot, "Por realizar", totales.pendiente(), false);
        JE_JE_SR_JA_ME_total(tot, "TOTAL DEL PLAN", totales.JE_JE_SR_JA_ME_total(), true);
        JE_JE_SR_JA_ME_total(tot, "Pagado a la fecha", cuenta.pagado(), false);
        JE_JE_SR_JA_ME_total(tot, "SALDO POR PAGAR", cuenta.JE_JE_SR_JA_ME_porPagarDelPlan(), true);
        doc.add(tot);
    }

    private void JE_JE_SR_JA_ME_total(PdfPTable t, String rotulo, BigDecimal monto, boolean destacado) {
        PdfPCell r = new PdfPCell(new Phrase(rotulo, JE_JE_SR_JA_ME_f(destacado ? 10 : 9, Font.BOLD, destacado ? TURQUESA : GRIS)));
        r.setColspan(4);
        r.setHorizontalAlignment(Element.ALIGN_RIGHT);
        r.setBorder(Rectangle.NO_BORDER);
        r.setPadding(5);
        PdfPCell v = new PdfPCell(new Phrase(JE_JE_SR_JA_ME_soles(monto), JE_JE_SR_JA_ME_f(destacado ? 11 : 9.5f, Font.BOLD, destacado ? TURQUESA : TINTA)));
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setBorder(destacado ? Rectangle.TOP : Rectangle.NO_BORDER);
        v.setBorderColor(TURQUESA);
        v.setPadding(5);
        t.addCell(r);
        t.addCell(v);
    }

    private void JE_JE_SR_JA_ME_encabezado(PdfPTable t, String... columnas) {
        for (String c : columnas) {
            PdfPCell h = new PdfPCell(new Phrase(c.toUpperCase(), JE_JE_SR_JA_ME_f(7.5f, Font.BOLD, GRIS)));
            h.setBackgroundColor(FONDO);
            h.setBorderColor(LINEA);
            h.setPadding(6);
            t.addCell(h);
        }
        t.setHeaderRows(1);
    }

    private void JE_JE_SR_JA_ME_fila(PdfPTable t, boolean alterna, String... valores) {
        for (int i = 0; i < valores.length; i++) {
            PdfPCell c = new PdfPCell(new Phrase(valores[i], JE_JE_SR_JA_ME_f(8.8f, i == 0 ? Font.BOLD : Font.NORMAL, TINTA)));
            c.setBorderColor(LINEA);
            c.setPadding(6);
            if (alterna) {
                c.setBackgroundColor(new Color(252, 253, 254));
            }
            if (i == valores.length - 1 && valores[i].startsWith("S/")) {
                c.setHorizontalAlignment(Element.ALIGN_RIGHT);
            }
            t.addCell(c);
        }
    }

    private void JE_JE_SR_JA_ME_firmas(Document doc, String conformidadPaciente) throws DocumentException {
        JE_JE_SR_JA_ME_Usuario u = sesion.JE_JE_SR_JA_ME_getUsuario();
        if (u != null && u.getRol() != com.dentavista.modelo.JE_JE_SR_JA_ME_Rol.ODONTOLOGO) {
            u = null; // solo firma con su nombre un odontólogo; si lo emite recepción o el paciente, queda genérico
        }
        PdfPTable t = new PdfPTable(new float[]{1, 0.3f, 1});
        t.setWidthPercentage(90);
        t.setSpacingBefore(46);
        t.addCell(JE_JE_SR_JA_ME_firma(u == null ? "Odontólogo tratante" : u.getNombre(),
                u != null && u.getColegiatura() != null ? u.getColegiatura() : "Firma y sello"));
        PdfPCell vacia = new PdfPCell();
        vacia.setBorder(Rectangle.NO_BORDER);
        t.addCell(vacia);
        t.addCell(JE_JE_SR_JA_ME_firma("Firma del paciente", conformidadPaciente));
        t.setKeepTogether(true);
        doc.add(t);
    }

    private PdfPCell JE_JE_SR_JA_ME_firma(String nombre, String detalle) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.TOP);
        c.setBorderColor(GRIS);
        c.setPaddingTop(6);
        Paragraph n = new Paragraph(nombre, JE_JE_SR_JA_ME_f(9, Font.BOLD, TINTA));
        n.setAlignment(Element.ALIGN_CENTER);
        Paragraph d = new Paragraph(detalle, JE_JE_SR_JA_ME_f(8, Font.NORMAL, GRIS));
        d.setAlignment(Element.ALIGN_CENTER);
        c.addElement(n);
        c.addElement(d);
        return c;
    }

    private static String JE_JE_SR_JA_ME_soles(BigDecimal m) {
        return "S/ " + new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(m);
    }

    /** Pie de página con número de página. */
    private static final class JE_JE_SR_JA_ME_Pie extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfContentByte cb = writer.getDirectContent();
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
                cb.beginText();
                cb.setFontAndSize(bf, 7.5f);
                cb.setColorFill(GRIS);
                cb.showTextAligned(Element.ALIGN_LEFT, "Generado con DentaVista", 36, 24, 0);
                cb.showTextAligned(Element.ALIGN_RIGHT, "Página " + writer.getPageNumber(),
                        document.getPageSize().getWidth() - 36, 24, 0);
                cb.endText();
            } catch (DocumentException | IOException ignorado) {
                // el pie es decorativo
            }
        }
    }
}
