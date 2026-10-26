package tse.cargauy.negocio.informe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import tse.cargauy.dtos.VolumenPorRubroDto;

class ExportadorPdfTest {

    private static Tabla<VolumenPorRubroDto> volumenes(VolumenPorRubroDto... filas) {
        return new Tabla<>(List.of(filas), List.of(
                new Columna<VolumenPorRubroDto>("columna.rubro", VolumenPorRubroDto::getRubro),
                new Columna<VolumenPorRubroDto>("columna.volumen", VolumenPorRubroDto::getVolumen)));
    }

    private static String texto(byte[] pdf) throws IOException {
        return new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1);
    }

    @Test
    void tieneElTituloLosDetallesYLaTabla() throws IOException {
        byte[] pdf = ExportadorPdf.exportar("Volumen transportado por rubro", List.of("Período: 01/10/2026 al 26/10/2026"),
                volumenes(new VolumenPorRubroDto("Agropecuario", 18000)));

        assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.US_ASCII));
        String texto = texto(pdf);
        assertTrue(texto.contains("Volumen transportado por rubro"));
        assertTrue(texto.contains("Período: 01/10/2026 al 26/10/2026"));
        assertTrue(texto.contains("Volumen (kg)"));
        assertTrue(texto.contains("Agropecuario"));
        assertTrue(texto.contains("18000"));
    }

    @Test
    void sinFilasInformaQueNoHayDatos() throws IOException {
        byte[] pdf = ExportadorPdf.exportar("Volumen transportado por rubro", List.of(), volumenes());

        assertTrue(texto(pdf).contains("Sin datos para los filtros seleccionados."));
    }
}
