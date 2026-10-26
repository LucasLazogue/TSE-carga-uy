package tse.cargauy.negocio.informe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.VolumenPorRubroDto;

class ExportadorCsvTest {

    private static final String BOM = String.valueOf((char) 0xFEFF);

    private static String csv(VolumenPorRubroDto... volumenes) {
        Tabla<VolumenPorRubroDto> tabla = new Tabla<>(List.of(volumenes), List.of(
                new Columna<VolumenPorRubroDto>("columna.rubro", VolumenPorRubroDto::getRubro),
                new Columna<VolumenPorRubroDto>("columna.volumen", VolumenPorRubroDto::getVolumen)));
        return new String(ExportadorCsv.exportar(tabla), StandardCharsets.UTF_8);
    }

    @Test
    void empiezaConBomYSeparaConPuntoYComaYCrlf() {
        assertEquals(BOM + "\"Rubro\";\"Volumen (kg)\"\r\n\"Agropecuario\";\"18000\"\r\n",
                csv(new VolumenPorRubroDto("Agropecuario", 18000)));
    }

    @Test
    void lasComillasSeEscapan() {
        assertEquals(BOM + "\"Rubro\";\"Volumen (kg)\"\r\n\"Carga \"\"general\"\"\";\"5\"\r\n",
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
