package tse.cargauy.data.testdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DatosPruebaTest {

    @Test
    void sinCargauyDatosCargaLaDemo() {
        assertEquals(DatosPrueba.PERFIL_DEMO, DatosPrueba.perfil(null));
        assertEquals(DatosPrueba.PERFIL_DEMO, DatosPrueba.perfil(" "));
    }

    @Test
    void aceptaBaseSinImportarMayusculas() {
        assertEquals(DatosPrueba.PERFIL_BASE, DatosPrueba.perfil(" Base "));
    }

    @Test
    void unPerfilDesconocidoNoArranca() {
        assertThrows(IllegalStateException.class, () -> DatosPrueba.perfil("performance"));
    }
}
