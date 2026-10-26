package tse.cargauy.negocio.informe;

import java.io.ByteArrayOutputStream;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfString;
import com.lowagie.text.pdf.PdfWriter;

public final class ExportadorPdf {

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font DETALLE = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font ENCABEZADO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font CELDA = FontFactory.getFont(FontFactory.HELVETICA, 10);

    private ExportadorPdf() {
    }

    public static byte[] exportar(String titulo, List<String> detalles, Tabla<?> tabla) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4);
        PdfWriter escritor = PdfWriter.getInstance(documento, salida);
        escritor.getExtraCatalog().put(PdfName.LANG, new PdfString("es-UY"));
        documento.addTitle(titulo);
        documento.open();
        documento.add(new Paragraph(titulo, TITULO));
        for (String detalle : detalles) {
            documento.add(new Paragraph(detalle, DETALLE));
        }
        if (tabla.isVacia()) {
            Paragraph sinDatos = new Paragraph(TextosInforme.resolver("informe.sinDatos"), DETALLE);
            sinDatos.setSpacingBefore(12);
            documento.add(sinDatos);
        } else {
            documento.add(tabla(tabla));
        }
        documento.close();
        return salida.toByteArray();
    }

    private static PdfPTable tabla(Tabla<?> tabla) {
        PdfPTable pdf = new PdfPTable(tabla.getEncabezados().size());
        pdf.setWidthPercentage(100);
        pdf.setSpacingBefore(12);
        pdf.setHeaderRows(1);
        for (String encabezado : tabla.getEncabezados()) {
            pdf.addCell(celda(encabezado, ENCABEZADO, false));
        }
        for (String[] fila : tabla.getValores()) {
            for (int j = 0; j < fila.length; j++) {
                pdf.addCell(celda(fila[j], CELDA, j > 0));
            }
        }
        return pdf;
    }

    private static PdfPCell celda(String texto, Font fuente, boolean aDerecha) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setPadding(4);
        if (aDerecha) {
            celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }
        return celda;
    }
}
