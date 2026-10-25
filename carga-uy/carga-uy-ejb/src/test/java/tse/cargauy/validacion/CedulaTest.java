package tse.cargauy.validacion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CedulaTest {

    @Test
    void aceptaCedulaConDigitoVerificadorCorrecto() {
        assertTrue(Cedula.esValida("12345672"));
        assertTrue(Cedula.esValida("55555555"));
    }

    @Test
    void aceptaCedulaDeSieteDigitos() {
        assertTrue(Cedula.esValida("9876549"));
    }

    @Test
    void rechazaDigitoVerificadorIncorrecto() {
        assertFalse(Cedula.esValida("12345678"));
    }

    @Test
    void rechazaFormatoInvalido() {
        assertFalse(Cedula.esValida(null));
        assertFalse(Cedula.esValida(""));
        assertFalse(Cedula.esValida("1.234.567-2"));
        assertFalse(Cedula.esValida("123456"));
        assertFalse(Cedula.esValida("123456789"));
    }
}
