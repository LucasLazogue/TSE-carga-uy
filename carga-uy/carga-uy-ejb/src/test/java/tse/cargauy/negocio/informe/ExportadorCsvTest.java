package tse.cargauy.negocio.informe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

class ExportadorCsvTest {

    private static String csv(String[]... filas) {
        return new String(ExportadorCsv.exportar(List.of(filas)), StandardCharsets.UTF_8);
    }

    @Test
    void empiezaConBomYSeparaConPuntoYComaYCrlf() {
        String csv = csv(new String[] { "Rubro", "Volumen (kg)" }, new String[] { "Agropecuario", "18000" });

        assertTrue(csv.startsWith("﻿"));
        assertEquals("﻿\"Rubro\";\"Volumen (kg)\"\r\n\"Agropecuario\";\"18000\"\r\n", csv);
    }

    @Test
    void lasComillasSeEscapan() {
        assertEquals("﻿\"Carga \"\"general\"\"\"\r\n", csv(new String[] { "Carga \"general\"" }));
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
