package tse.cargauy.adaptadores.pdi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class FechaDnicTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 8);

    @Test
    void normalizaElFormatoDeDnic() {
        assertEquals("1985-03-12", FechaDnic.normalizar("1985-0312"));
    }

    @Test
    void aceptaLaFechaConGuiones() {
        assertEquals("1985-03-12", FechaDnic.normalizar("1985-03-12"));
    }

    @Test
    void aceptaAnioMesYSoloAnio() {
        assertEquals("1985-03", FechaDnic.normalizar("1985-03"));
        assertEquals("1985", FechaDnic.normalizar("1985"));
    }

    @Test
    void vaciaOInvalidaEsNull() {
        assertNull(FechaDnic.normalizar(null));
        assertNull(FechaDnic.normalizar(""));
        assertNull(FechaDnic.normalizar("   "));
        assertNull(FechaDnic.normalizar("12/03/1985"));
        assertNull(FechaDnic.normalizar("1985-1345"));
        assertNull(FechaDnic.normalizar("1985-13"));
    }

    @Test
    void elDiaQueCumpleDieciochoYaNoEsMenor() {
        assertFalse(FechaDnic.esMenor("2008-11-08", HOY));
    }

    @Test
    void elDiaAntesDeCumplirDieciochoEsMenor() {
        assertTrue(FechaDnic.esMenor("2008-11-09", HOY));
    }

    @Test
    void sinFechaNoSeConsideraMenor() {
        assertFalse(FechaDnic.esMenor(null, HOY));
    }

    @Test
    void conSoloElAnioUsaElUltimoDiaDelAnio() {
        assertTrue(FechaDnic.esMenor("2008", HOY));
        assertFalse(FechaDnic.esMenor("2008", LocalDate.of(2026, 12, 31)));
        assertFalse(FechaDnic.esMenor("2007", HOY));
    }

    @Test
    void conAnioMesUsaElUltimoDiaDelMes() {
        assertTrue(FechaDnic.esMenor("2008-11", HOY));
        assertFalse(FechaDnic.esMenor("2008-11", LocalDate.of(2026, 11, 30)));
        assertFalse(FechaDnic.esMenor("2008-10", HOY));
    }
}
