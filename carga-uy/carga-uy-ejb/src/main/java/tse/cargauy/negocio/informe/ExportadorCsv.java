package tse.cargauy.negocio.informe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

public final class ExportadorCsv {

    private static final CSVFormat FORMATO = CSVFormat.EXCEL.builder()
            .setDelimiter(';')
            .setRecordSeparator("\r\n")
            .setQuoteMode(QuoteMode.ALL)
            .get();

    private ExportadorCsv() {
    }

    public static byte[] exportar(List<Seccion> secciones) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (Writer escritor = new OutputStreamWriter(salida, StandardCharsets.UTF_8);
             CSVPrinter impresora = new CSVPrinter(escritor, FORMATO)) {
            escritor.write(0xFEFF);
            imprimir(impresora, TextosInforme.resolver("columna.reporte"), TextosInforme.resolver("columna.concepto"),
                    TextosInforme.resolver("columna.medida"), TextosInforme.resolver("columna.valor"));
            for (Seccion seccion : secciones) {
                List<String> encabezados = seccion.getTabla().getEncabezados();
                for (String[] fila : seccion.getTabla().getValores()) {
                    for (int j = 1; j < fila.length; j++) {
                        imprimir(impresora, seccion.getTitulo(), fila[0], encabezados.get(j), fila[j]);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return salida.toByteArray();
    }

    private static void imprimir(CSVPrinter impresora, String... celdas) throws IOException {
        impresora.printRecord(Arrays.stream(celdas).map(ExportadorCsv::neutralizar).toList());
    }

    static String neutralizar(String celda) {
        if (!celda.isEmpty() && "=+-@\t\r".indexOf(celda.charAt(0)) >= 0) {
            return "'" + celda;
        }
        return celda;
    }
}
