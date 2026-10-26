package tse.cargauy.negocio.informe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

    public static byte[] exportar(Tabla<?> tabla) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (Writer escritor = new OutputStreamWriter(salida, StandardCharsets.UTF_8);
             CSVPrinter impresora = new CSVPrinter(escritor, FORMATO)) {
            escritor.write(0xFEFF);
            impresora.printRecord(tabla.getEncabezados().stream().map(ExportadorCsv::neutralizar).toList());
            for (String[] fila : tabla.getValores()) {
                impresora.printRecord(Arrays.stream(fila).map(ExportadorCsv::neutralizar).toList());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return salida.toByteArray();
    }

    static String neutralizar(String celda) {
        if (!celda.isEmpty() && "=+-@\t\r".indexOf(celda.charAt(0)) >= 0) {
            return "'" + celda;
        }
        return celda;
    }
}
