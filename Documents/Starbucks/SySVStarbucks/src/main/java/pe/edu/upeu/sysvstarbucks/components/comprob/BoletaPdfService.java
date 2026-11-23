package pe.edu.upeu.sysvstarbucks.components.comprob;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import pe.edu.upeu.sysvstarbucks.dto.comprobante.BoletaVenta;
import pe.edu.upeu.sysvstarbucks.dto.comprobante.ItemBoleta;

import java.awt.Color;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BoletaPdfService {
    private static final Color PRIMARIO = new Color(0x2c, 0x3e, 0x50);
    private static final Color ACENTO = new Color(0xc0, 0x39, 0x2b);
    private static final Color CLARO = new Color(0xf4, 0xf6, 0xf7);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10, PRIMARIO);
    private static final Font NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARIO);
    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, PRIMARIO);
    private static final Font ACENTO_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, ACENTO);
    private static final Font BLANCO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

    public void exportar(BoletaVenta boleta, Path destino) throws Exception {
        try (OutputStream salida = Files.newOutputStream(destino)) { exportar(boleta, salida); }
    }

    public void exportar(BoletaVenta boleta, OutputStream salida) throws Exception {
        Document doc = new Document(PageSize.A4, 38, 38, 38, 38);
        try {
            PdfWriter.getInstance(doc, salida);
            doc.open();
            doc.add(cabecera(boleta)); doc.add(Chunk.NEWLINE);
            doc.add(datosCliente(boleta)); doc.add(Chunk.NEWLINE);
            doc.add(detalle(boleta)); doc.add(Chunk.NEWLINE);
            doc.add(totales(boleta)); doc.add(Chunk.NEWLINE);
            doc.add(pie());
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    private PdfPTable cabecera(BoletaVenta b) {
        PdfPTable tabla = new PdfPTable(new float[]{58, 42});
        tabla.setWidthPercentage(100);
        PdfPCell emisor = new PdfPCell(); emisor.setBorder(Rectangle.NO_BORDER);
        emisor.addElement(new Paragraph("STARBUCKS", TITULO));
        emisor.addElement(new Paragraph("RUC: " + BoletaVenta.RUC_EMISOR, NEGRITA));
        emisor.addElement(new Paragraph(BoletaVenta.DIRECCION_EMISOR, NORMAL));

        PdfPCell doc = new PdfPCell(); doc.setBorderColor(ACENTO); doc.setBorderWidth(2); doc.setPadding(9);
        centered(doc, "BOLETA DE VENTA", ACENTO_BOLD);
        centered(doc, "R.U.C. " + BoletaVenta.RUC_EMISOR, TITULO);
        centered(doc, "Nro. " + b.codigo(), ACENTO_BOLD);
        centered(doc, "Fecha: " + b.fechaEmisionTexto() + " | Moneda: PEN", NORMAL);
        tabla.addCell(emisor); tabla.addCell(doc);
        return tabla;
    }

    private PdfPTable datosCliente(BoletaVenta b) {
        PdfPTable tabla = new PdfPTable(new float[]{25, 75});
        tabla.setWidthPercentage(100);
        tabla.addCell(banda("DATOS DEL CLIENTE", 2));
        fila(tabla, b.tipoDocumentoCliente(), b.numeroDocumentoCliente());
        fila(tabla, "Cliente", b.nombreCliente());
        fila(tabla, "Dirección", b.direccionCliente());
        return tabla;
    }

    private PdfPTable detalle(BoletaVenta b) {
        PdfPTable tabla = new PdfPTable(new float[]{10, 48, 21, 21});
        tabla.setWidthPercentage(100);
        tabla.addCell(banda("DETALLE", 4));
        tabla.addCell(celdaCabecera("CANT.", Element.ALIGN_CENTER));
        tabla.addCell(celdaCabecera("DESCRIPCIÓN", Element.ALIGN_LEFT));
        tabla.addCell(celdaCabecera("P. UNIT.", Element.ALIGN_RIGHT));
        tabla.addCell(celdaCabecera("SUBTOTAL", Element.ALIGN_RIGHT));
        for (int i = 0; i < b.items().size(); i++) {
            ItemBoleta item = b.items().get(i);
            Color fondo = i % 2 == 0 ? Color.WHITE : CLARO;
            tabla.addCell(celdaDato(String.valueOf(item.cantidad()), Element.ALIGN_CENTER, fondo));
            tabla.addCell(celdaDato(item.descripcion(), Element.ALIGN_LEFT, fondo));
            tabla.addCell(celdaDato("S/ " + BoletaVenta.dinero(item.precioUnitario()), Element.ALIGN_RIGHT, fondo));
            tabla.addCell(celdaDato("S/ " + BoletaVenta.dinero(item.subtotal()), Element.ALIGN_RIGHT, fondo));
        }
        return tabla;
    }

    private PdfPTable totales(BoletaVenta b) {
        PdfPTable tabla = new PdfPTable(new float[]{55, 45});
        tabla.setWidthPercentage(100);
        PdfPCell izquierda = new PdfPCell(); izquierda.setBorder(Rectangle.NO_BORDER);
        izquierda.addElement(new Paragraph("SON:", NEGRITA));
        izquierda.addElement(new Paragraph("TOTAL: S/ " + BoletaVenta.dinero(b.total()), NEGRITA));
        izquierda.addElement(new Paragraph("Gracias por tu compra", NORMAL));
        tabla.addCell(izquierda);

        PdfPTable montos = new PdfPTable(new float[]{58, 42});
        montos.setWidthPercentage(100);
        agregarMonto(montos, "Op. Gravadas", "S/ " + BoletaVenta.dinero(b.opGravadas()), false);
        agregarMonto(montos, "IGV (18%)", "S/ " + BoletaVenta.dinero(b.igv()), false);
        agregarMonto(montos, "TOTAL", "S/ " + BoletaVenta.dinero(b.total()), true);
        PdfPCell derecha = new PdfPCell(montos); derecha.setBorder(Rectangle.NO_BORDER);
        tabla.addCell(derecha);
        return tabla;
    }

    private Paragraph pie() {
        Paragraph pie = new Paragraph(
                "Representación impresa de la BOLETA DE VENTA.", NORMAL);
        pie.setAlignment(Element.ALIGN_CENTER);
        return pie;
    }

    private PdfPCell banda(String texto, int columnas) {
        PdfPCell c = new PdfPCell(new Phrase(texto, BLANCO)); c.setColspan(columnas);
        c.setBackgroundColor(PRIMARIO); c.setPadding(6); c.setBorder(Rectangle.NO_BORDER);
        return c;
    }
    private PdfPCell celdaCabecera(String texto, int alineacion) {
        PdfPCell c = new PdfPCell(new Phrase(texto, BLANCO)); c.setBackgroundColor(PRIMARIO);
        c.setPadding(6); c.setHorizontalAlignment(alineacion); c.setBorderColor(CLARO);
        return c;
    }
    private PdfPCell celdaDato(String texto, int alineacion, Color fondo) {
        PdfPCell c = new PdfPCell(new Phrase(texto, NORMAL)); c.setBackgroundColor(fondo);
        c.setPadding(6); c.setHorizontalAlignment(alineacion); c.setBorderColor(CLARO);
        return c;
    }
    private void fila(PdfPTable tabla, String campo, String valor) {
        tabla.addCell(celdaDato(campo, Element.ALIGN_LEFT, CLARO));
        tabla.addCell(celdaDato(valor, Element.ALIGN_LEFT, Color.WHITE));
    }
    private void agregarMonto(PdfPTable tabla, String concepto, String valor, boolean total) {
        Font fuente = total ? BLANCO : NEGRITA;
        Color fondo = total ? ACENTO : CLARO;
        PdfPCell etiqueta = new PdfPCell(new Phrase(concepto, fuente));
        etiqueta.setBackgroundColor(fondo); etiqueta.setPadding(6); etiqueta.setBorderColor(CLARO);
        PdfPCell monto = new PdfPCell(new Phrase(valor, fuente));
        monto.setBackgroundColor(fondo); monto.setPadding(6); monto.setBorderColor(CLARO);
        monto.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(etiqueta); tabla.addCell(monto);
    }
    private void centered(PdfPCell celda, String texto, Font fuente) {
        Paragraph p = new Paragraph(texto, fuente); p.setAlignment(Element.ALIGN_CENTER); celda.addElement(p);
    }
}
