package tse.cargauy.negocio.informe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.VolumenPorRubroDto;

class ExportadorCsvTest {

    private static final String BOM = String.valueOf((char) 0xFEFF);
    private static final String ENCABEZADO = "\"Reporte\";\"Concepto\";\"Medida\";\"Valor\"\r\n";

    private static String csv(VolumenPorRubroDto... volumenes) {
        Tabla<VolumenPorRubroDto> tabla = new Tabla<>(List.of(volumenes), List.of(
                new Columna<VolumenPorRubroDto>("columna.rubro", VolumenPorRubroDto::getRubro),
                new Columna<VolumenPorRubroDto>("columna.volumen", VolumenPorRubroDto::getVolumen)));
        return new String(ExportadorCsv.exportar(List.of(new Seccion("seccion.volumen", tabla))), StandardCharsets.UTF_8);
    }

    @Test
    void cadaDatoEsUnaFilaConSuReporteConceptoYMedida() {
        assertEquals(BOM + ENCABEZADO
                        + "\"Volumen transportado por rubro\";\"Agropecuario\";\"Volumen (kg)\";\"18000\"\r\n"
                        + "\"Volumen transportado por rubro\";\"Forestal\";\"Volumen (kg)\";\"500\"\r\n",
                csv(new VolumenPorRubroDto("Agropecuario", 18000), new VolumenPorRubroDto("Forestal", 500)));
    }

    @Test
    void sinDatosQuedaSoloElEncabezado() {
        assertEquals(BOM + ENCABEZADO, csv());
    }

    @Test
    void lasComillasSeEscapan() {
        assertEquals(BOM + ENCABEZADO + "\"Volumen transportado por rubro\";\"Carga \"\"general\"\"\";\"Volumen (kg)\";\"5\"\r\n",
                csv(new VolumenPorRubroDto("Carga \"general\"", 5)));
    }

    @Test
    void lasCeldasQuePodrianSerFormulasSeNeutralizan() {
        for (String celda : List.of("=1+1", "+1", "-1", "@SUMA(A1)", "\tx", "\rx")) {
            assertEquals("'" + celda, ExportadorCsv.neutralizar(celda));
        }
        assertEquals("Agropecuario", ExportadorCsv.neutralizar("Agropecuario"));
        assertEquals("", ExportadorCsv.neutralizar(""));
    }
}
