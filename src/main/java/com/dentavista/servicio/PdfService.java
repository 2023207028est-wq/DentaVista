package com.dentavista.servicio;

import com.dentavista.dominio.EstadoDental;
import com.dentavista.dominio.Odontograma;
import com.dentavista.dominio.Pieza;
import com.dentavista.modelo.EstadoItem;
import com.dentavista.modelo.ItemPlan;
import com.dentavista.modelo.Paciente;
import com.dentavista.modelo.Pago;
import com.dentavista.modelo.Receta;
import com.dentavista.modelo.Usuario;
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
 * Informe odontológico y presupuesto en PDF: datos del paciente con alertas,
 * odontograma 3D (tres vistas) y 2D, hallazgos actuales y plan de tratamiento con
 * importes, listo para imprimir y firmar.
 */
@Service
@RequiredArgsConstructor
public class PdfService {

    /** Imágenes PNG ya renderizadas del odontograma. */
    public record Imagenes(byte[] frontal, byte[] superior, byte[] inferior, byte[] odontograma2d) {
    }

    private static final Color TURQUESA = new Color(13, 148, 136);
    private static final Color TINTA = new Color(15, 23, 42);
    private static final Color GRIS = new Color(100, 116, 139);
    private static final Color LINEA = new Color(226, 232, 240);
    private static final Color FONDO = new Color(248, 250, 252);
    private static final Color ROJO = new Color(185, 28, 28);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DatosClinica clinica;
    private final SesionService sesion;

    private Font f(float tam, int estilo, Color c) {
        return new Font(Font.HELVETICA, tam, estilo, c);
    }

    public Path generar(Paciente p, Odontograma o, List<ItemPlan> items, PlanService.Totales totales,
                        PagoService.Cuenta cuenta, Imagenes img) {
        try {
            Path carpeta = carpetaSalida();
            String nombre = "DentaVista_" + p.getApellidos().split(" ")[0] + "_" + p.getDni() + "_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf";
            Path destino = carpeta.resolve(nombre);
            try (OutputStream out = Files.newOutputStream(destino)) {
                escribir(out, p, o, items, totales, cuenta, img);
            }
            return destino;
        } catch (IOException | DocumentException e) {
            throw new NegocioException("No se pudo generar el PDF: " + e.getMessage());
        }
    }

    /** Documentos\DentaVista (o la carpeta personal si no existe Documentos). */
    public static Path carpetaSalida() throws IOException {
        Path home = Path.of(System.getProperty("user.home"));
        Path docs = home.resolve("Documents");
        Path carpeta = (Files.isDirectory(docs) ? docs : home).resolve("DentaVista");
        Files.createDirectories(carpeta);
        return carpeta;
    }

    void escribir(OutputStream out, Paciente p, Odontograma o, List<ItemPlan> items,
                  PlanService.Totales totales, PagoService.Cuenta cuenta, Imagenes img) throws DocumentException, IOException {
        Document doc = new Document(PageSize.A4, 36, 36, 34, 46);
        PdfWriter w = PdfWriter.getInstance(doc, out);
        w.setPageEvent(new Pie());
        doc.addTitle("Informe odontológico - " + p.nombreCompleto());
        doc.addCreator("DentaVista");
        doc.open();

        cabecera(doc);
        datosPaciente(doc, p);

        titulo(doc, "Odontograma 3D");
        PdfPTable vistas = new PdfPTable(new float[]{1.45f, 1f});
        vistas.setWidthPercentage(100);
        PdfPCell grande = celdaImagen(img.frontal(), "Vista frontal");
        grande.setRowspan(2);
        vistas.addCell(grande);
        vistas.addCell(celdaImagen(img.superior(), "Arcada superior (oclusal)"));
        vistas.addCell(celdaImagen(img.inferior(), "Arcada inferior (oclusal)"));
        doc.add(vistas);

        titulo(doc, "Odontograma clínico (notación FDI)");
        Image i2d = Image.getInstance(img.odontograma2d());
        i2d.scaleToFit(PageSize.A4.getWidth() - 72, 190);
        i2d.setAlignment(Element.ALIGN_CENTER);
        doc.add(i2d);
        leyenda(doc);

        // Las imágenes llenan la primera hoja: el detalle clínico empieza en una nueva.
        doc.newPage();
        titulo(doc, "Hallazgos actuales");
        hallazgos(doc, o);

        titulo(doc, "Plan de tratamiento y presupuesto");
        plan(doc, items, totales, cuenta);

        Paragraph validez = new Paragraph("Presupuesto válido por 30 días. Los importes están expresados en soles (S/) "
                + "e incluyen IGV. El plan puede ajustarse según la evolución clínica.", f(8.5f, Font.ITALIC, GRIS));
        validez.setSpacingBefore(8);
        doc.add(validez);

        firmas(doc);
        doc.close();
    }

    // ------------------------------------------------------------- recibo

    /** Recibo de pago (A5) con el estado de cuenta después del abono. */
    public Path recibo(Pago pago, PagoService.Cuenta cuenta) {
        try {
            Path destino = carpetaSalida().resolve("Recibo_" + pago.getRecibo() + ".pdf");
            try (OutputStream out = Files.newOutputStream(destino)) {
                Document doc = new Document(PageSize.A5, 30, 30, 28, 40);
                PdfWriter w = PdfWriter.getInstance(doc, out);
                w.setPageEvent(new Pie());
                doc.open();
                cabecera(doc, "RECIBO DE PAGO\n" + pago.getRecibo(), pago.getFecha().format(FECHA_HORA), 12);
                Paciente p = pago.getPaciente();
                PdfPTable d = new PdfPTable(2);
                d.setWidthPercentage(100);
                d.setSpacingBefore(10);
                dato(d, "PACIENTE", p.nombreCompleto());
                dato(d, "DNI", p.getDni());
                doc.add(d);

                PdfPTable t = new PdfPTable(new float[]{2.4f, 1f});
                t.setWidthPercentage(100);
                t.setSpacingBefore(12);
                PdfPCell c1 = new PdfPCell(new Phrase("Abono a plan de tratamiento odontológico"
                        + (pago.getNota() != null ? "\n" + pago.getNota() : "") + "\nMedio de pago: " + pago.getMedio().getEtiqueta(),
                        f(9.5f, Font.NORMAL, TINTA)));
                c1.setPadding(8);
                c1.setBorderColor(LINEA);
                PdfPCell c2 = new PdfPCell(new Phrase(soles(pago.getMonto()), f(14, Font.BOLD, TURQUESA)));
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
                filaCuenta(cta, "Total del plan", cuenta.totalPlan(), false);
                filaCuenta(cta, "Tratamientos realizados", cuenta.realizado(), false);
                filaCuenta(cta, "Total pagado a la fecha", cuenta.pagado(), false);
                if (cuenta.deuda().signum() > 0) {
                    filaCuenta(cta, "Debe por tratamientos realizados", cuenta.deuda(), false);
                } else if (cuenta.aFavor().signum() > 0) {
                    filaCuenta(cta, "Adelanto (pagado, aún no realizado)", cuenta.aFavor(), false);
                }
                filaCuenta(cta, "Saldo por pagar del plan", cuenta.porPagarDelPlan(), true);
                doc.add(cta);
                if (pago.isAnulado()) {
                    Paragraph an = new Paragraph("RECIBO ANULADO: " + pago.getMotivoAnulacion(), f(11, Font.BOLD, ROJO));
                    an.setSpacingBefore(10);
                    doc.add(an);
                }
                PdfPTable fir = new PdfPTable(1);
                fir.setWidthPercentage(55);
                fir.setSpacingBefore(40);
                fir.addCell(firma(pago.getRegistradoPor() == null ? "Caja" : pago.getRegistradoPor(), "Recibido por"));
                doc.add(fir);
                doc.close();
            }
            return destino;
        } catch (IOException | DocumentException e) {
            throw new NegocioException("No se pudo generar el recibo: " + e.getMessage());
        }
    }

    private void filaCuenta(PdfPTable t, String rotulo, BigDecimal monto, boolean destacado) {
        PdfPCell a = new PdfPCell(new Phrase(rotulo, f(destacado ? 10 : 9, Font.BOLD, destacado ? TURQUESA : GRIS)));
        a.setBorder(destacado ? Rectangle.TOP : Rectangle.NO_BORDER);
        a.setBorderColor(TURQUESA);
        a.setPadding(4);
        PdfPCell b = new PdfPCell(new Phrase(soles(monto), f(destacado ? 11 : 9.5f, Font.BOLD, destacado ? TURQUESA : TINTA)));
        b.setBorder(destacado ? Rectangle.TOP : Rectangle.NO_BORDER);
        b.setBorderColor(TURQUESA);
        b.setHorizontalAlignment(Element.ALIGN_RIGHT);
        b.setPadding(4);
        t.addCell(a);
        t.addCell(b);
    }

    // ------------------------------------------------------------- receta

    /** Receta médica (A5) con alergias destacadas y firma del odontólogo. */
    public Path receta(Receta r) {
        try {
            Path destino = carpetaSalida().resolve("Receta_" + r.getPaciente().getDni() + "_"
                    + r.getFecha().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
            try (OutputStream out = Files.newOutputStream(destino)) {
                Document doc = new Document(PageSize.A5, 30, 30, 28, 40);
                PdfWriter w = PdfWriter.getInstance(doc, out);
                w.setPageEvent(new Pie());
                doc.open();
                cabecera(doc, "RECETA", r.getFecha().format(FECHA_HORA), 12);
                Paciente p = r.getPaciente();
                PdfPTable d = new PdfPTable(new float[]{2.2f, 1f, 0.8f});
                d.setWidthPercentage(100);
                d.setSpacingBefore(10);
                dato(d, "PACIENTE", p.nombreCompleto());
                dato(d, "DNI", p.getDni());
                dato(d, "EDAD", p.edad() == null ? "—" : p.edad() + " años");
                doc.add(d);
                if (p.getAlergias() != null) {
                    Paragraph al = new Paragraph("ALERGIAS: " + p.getAlergias(), f(9, Font.BOLD, ROJO));
                    al.setSpacingBefore(6);
                    doc.add(al);
                }
                Paragraph rp = new Paragraph("Rp.", f(16, Font.BOLDITALIC, TURQUESA));
                rp.setSpacingBefore(12);
                doc.add(rp);
                int n = 1;
                for (Receta.Item i : r.getItems()) {
                    Paragraph m = new Paragraph(n++ + ".  " + i.getMedicamento(), f(10.5f, Font.BOLD, TINTA));
                    m.setSpacingBefore(6);
                    doc.add(m);
                    doc.add(new Paragraph("     " + i.getIndicacion(), f(9.5f, Font.NORMAL, TINTA)));
                }
                if (r.getIndicaciones() != null) {
                    Paragraph t = new Paragraph("Indicaciones: " + r.getIndicaciones(), f(9, Font.ITALIC, GRIS));
                    t.setSpacingBefore(12);
                    doc.add(t);
                }
                Usuario u = r.getOdontologo();
                PdfPTable fir = new PdfPTable(1);
                fir.setWidthPercentage(60);
                fir.setSpacingBefore(46);
                fir.addCell(firma(u.getNombre(), u.getColegiatura() == null ? "Cirujano dentista"
                        : "Cirujano dentista · " + u.getColegiatura()));
                doc.add(fir);
                doc.close();
            }
            return destino;
        } catch (IOException | DocumentException e) {
            throw new NegocioException("No se pudo generar la receta: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- bloques

    private void cabecera(Document doc) throws DocumentException {
        cabecera(doc, "INFORME ODONTOLÓGICO\nY PRESUPUESTO", "Emitido el " + LocalDateTime.now().format(FECHA_HORA), 15);
    }

    private void cabecera(Document doc, String titulo, String detalle, float tamNombre) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{1.3f, 1f});
        t.setWidthPercentage(100);
        PdfPCell izq = new PdfPCell();
        izq.setBorder(Rectangle.NO_BORDER);
        izq.addElement(new Paragraph(clinica.nombre(), f(tamNombre, Font.BOLD, TURQUESA)));
        izq.addElement(new Paragraph("RUC " + clinica.ruc() + " · " + clinica.direccion(), f(8.5f, Font.NORMAL, GRIS)));
        izq.addElement(new Paragraph(clinica.telefono() + " · " + clinica.email(), f(8.5f, Font.NORMAL, GRIS)));
        t.addCell(izq);

        PdfPCell der = new PdfPCell();
        der.setBorder(Rectangle.NO_BORDER);
        der.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph tit = new Paragraph(titulo, f(12, Font.BOLD, TINTA));
        tit.setAlignment(Element.ALIGN_RIGHT);
        der.addElement(tit);
        Paragraph fecha = new Paragraph(detalle, f(8.5f, Font.NORMAL, GRIS));
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

    private void datosPaciente(Document doc, Paciente p) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{1, 1, 1, 1});
        t.setWidthPercentage(100);
        t.setSpacingBefore(10);
        dato(t, "PACIENTE", p.nombreCompleto());
        dato(t, "DNI", p.getDni());
        dato(t, "EDAD", p.edad() == null ? "—" : p.edad() + " años");
        dato(t, "TELÉFONO", p.getTelefono() == null ? "—" : p.getTelefono());
        doc.add(t);
        if (p.getAlergias() != null || p.getAntecedentes() != null) {
            PdfPTable a = new PdfPTable(1);
            a.setWidthPercentage(100);
            a.setSpacingBefore(4);
            Phrase ph = new Phrase();
            if (p.getAlergias() != null) {
                ph.add(new Chunk("ALERGIA: ", f(9, Font.BOLD, ROJO)));
                ph.add(new Chunk(p.getAlergias() + "    ", f(9, Font.NORMAL, ROJO)));
            }
            if (p.getAntecedentes() != null) {
                ph.add(new Chunk("ANTECEDENTES: ", f(9, Font.BOLD, new Color(180, 83, 9))));
                ph.add(new Chunk(p.getAntecedentes(), f(9, Font.NORMAL, new Color(180, 83, 9))));
            }
            PdfPCell c = new PdfPCell(ph);
            c.setBackgroundColor(new Color(254, 242, 242));
            c.setBorderColor(new Color(254, 202, 202));
            c.setPadding(7);
            a.addCell(c);
            doc.add(a);
        }
    }

    private void dato(PdfPTable t, String rotulo, String valor) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setBackgroundColor(FONDO);
        c.setPadding(7);
        c.addElement(new Paragraph(rotulo, f(7.5f, Font.BOLD, GRIS)));
        c.addElement(new Paragraph(valor, f(10, Font.BOLD, TINTA)));
        t.addCell(c);
    }

    private void titulo(Document doc, String texto) throws DocumentException {
        Paragraph p = new Paragraph(texto.toUpperCase(), f(9.5f, Font.BOLD, TURQUESA));
        p.setSpacingBefore(12);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private PdfPCell celdaImagen(byte[] png, String pie) throws IOException, DocumentException {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(3);
        Image im = Image.getInstance(png);
        im.setWidthPercentage(100);
        c.addElement(im);
        Paragraph l = new Paragraph(pie, f(7.5f, Font.NORMAL, GRIS));
        l.setAlignment(Element.ALIGN_CENTER);
        c.addElement(l);
        return c;
    }

    private void leyenda(Document doc) throws DocumentException {
        Phrase ph = new Phrase();
        for (EstadoDental e : EstadoDental.values()) {
            if (e == EstadoDental.SANO) {
                continue;
            }
            java.awt.Color c = java.awt.Color.decode(e.getColor());
            ph.add(new Chunk("■ ", f(10, Font.BOLD, c)));
            ph.add(new Chunk(e.getEtiqueta() + "     ", f(7.5f, Font.NORMAL, GRIS)));
        }
        Paragraph p = new Paragraph(ph);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
    }

    private void hallazgos(Document doc, Odontograma o) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{0.5f, 2.2f, 3.3f});
        t.setWidthPercentage(100);
        encabezado(t, "Pieza", "Descripción", "Estado");
        int filas = 0;
        for (Odontograma.EstadoPieza ep : o.piezas()) {
            if (ep.sana()) {
                continue;
            }
            fila(t, filas++ % 2 == 1, String.valueOf(ep.fdi()), Pieza.nombre(ep.fdi()), ep.descripcion());
        }
        if (filas == 0) {
            PdfPCell c = new PdfPCell(new Phrase("Sin hallazgos: todas las piezas presentes están sanas.", f(9, Font.ITALIC, GRIS)));
            c.setColspan(3);
            c.setPadding(8);
            c.setBorderColor(LINEA);
            t.addCell(c);
        }
        doc.add(t);
    }

    private void plan(Document doc, List<ItemPlan> items, PlanService.Totales totales,
                      PagoService.Cuenta cuenta) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{0.35f, 1.1f, 2.6f, 0.95f, 0.9f});
        t.setWidthPercentage(100);
        encabezado(t, "#", "Pieza / cara", "Tratamiento", "Estado", "Importe");
        int n = 0;
        for (ItemPlan i : items) {
            if (i.getEstado() == EstadoItem.CANCELADO) {
                continue;
            }
            n++;
            fila(t, n % 2 == 0, String.valueOf(n), i.ubicacion(), i.getTratamiento().getNombre()
                    + (i.getNota() != null ? "\n" + i.getNota() : ""), i.getEstado().getEtiqueta(), soles(i.getPrecio()));
        }
        if (n == 0) {
            PdfPCell c = new PdfPCell(new Phrase("No hay tratamientos en el plan.", f(9, Font.ITALIC, GRIS)));
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
        total(tot, "Realizado", totales.realizado(), false);
        total(tot, "Por realizar", totales.pendiente(), false);
        total(tot, "TOTAL DEL PLAN", totales.total(), true);
        total(tot, "Pagado a la fecha", cuenta.pagado(), false);
        total(tot, "SALDO POR PAGAR", cuenta.porPagarDelPlan(), true);
        doc.add(tot);
    }

    private void total(PdfPTable t, String rotulo, BigDecimal monto, boolean destacado) {
        PdfPCell r = new PdfPCell(new Phrase(rotulo, f(destacado ? 10 : 9, Font.BOLD, destacado ? TURQUESA : GRIS)));
        r.setColspan(4);
        r.setHorizontalAlignment(Element.ALIGN_RIGHT);
        r.setBorder(Rectangle.NO_BORDER);
        r.setPadding(5);
        PdfPCell v = new PdfPCell(new Phrase(soles(monto), f(destacado ? 11 : 9.5f, Font.BOLD, destacado ? TURQUESA : TINTA)));
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setBorder(destacado ? Rectangle.TOP : Rectangle.NO_BORDER);
        v.setBorderColor(TURQUESA);
        v.setPadding(5);
        t.addCell(r);
        t.addCell(v);
    }

    private void encabezado(PdfPTable t, String... columnas) {
        for (String c : columnas) {
            PdfPCell h = new PdfPCell(new Phrase(c.toUpperCase(), f(7.5f, Font.BOLD, GRIS)));
            h.setBackgroundColor(FONDO);
            h.setBorderColor(LINEA);
            h.setPadding(6);
            t.addCell(h);
        }
        t.setHeaderRows(1);
    }

    private void fila(PdfPTable t, boolean alterna, String... valores) {
        for (int i = 0; i < valores.length; i++) {
            PdfPCell c = new PdfPCell(new Phrase(valores[i], f(8.8f, i == 0 ? Font.BOLD : Font.NORMAL, TINTA)));
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

    private void firmas(Document doc) throws DocumentException {
        Usuario u = sesion.getUsuario();
        if (u != null && u.getRol() == com.dentavista.modelo.Rol.PACIENTE) {
            u = null; // el paciente descarga su informe: la firma es del odontólogo tratante
        }
        PdfPTable t = new PdfPTable(new float[]{1, 0.3f, 1});
        t.setWidthPercentage(90);
        t.setSpacingBefore(46);
        t.addCell(firma(u == null ? "Odontólogo tratante" : u.getNombre(),
                u != null && u.getColegiatura() != null ? u.getColegiatura() : "Odontólogo tratante"));
        PdfPCell vacia = new PdfPCell();
        vacia.setBorder(Rectangle.NO_BORDER);
        t.addCell(vacia);
        t.addCell(firma("Firma del paciente", "Acepto el plan y presupuesto"));
        t.setKeepTogether(true);
        doc.add(t);
    }

    private PdfPCell firma(String nombre, String detalle) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.TOP);
        c.setBorderColor(GRIS);
        c.setPaddingTop(6);
        Paragraph n = new Paragraph(nombre, f(9, Font.BOLD, TINTA));
        n.setAlignment(Element.ALIGN_CENTER);
        Paragraph d = new Paragraph(detalle, f(8, Font.NORMAL, GRIS));
        d.setAlignment(Element.ALIGN_CENTER);
        c.addElement(n);
        c.addElement(d);
        return c;
    }

    private static String soles(BigDecimal m) {
        return "S/ " + new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(m);
    }

    /** Pie de página con número de página. */
    private static final class Pie extends PdfPageEventHelper {
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
