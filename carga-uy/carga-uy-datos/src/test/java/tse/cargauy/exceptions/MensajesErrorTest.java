package tse.cargauy.exceptions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MensajesErrorTest {

    @Test
    void todoCodigoTieneMensaje() {
        for (CodigoError codigo : CodigoError.values()) {
            assertNotEquals(codigo.name(), MensajesError.resolver(codigo),
                    "falta la clave " + codigo.name() + " en mensajes-error.properties");
        }
    }

    @Test
    void losParametrosSeReemplazan() {
        String mensaje = MensajesError.resolver(CodigoError.VEHICULO_MATRICULA_DUPLICADA, "STA1234");

        assertTrue(mensaje.contains("STA1234"));
        assertFalse(mensaje.contains("{0}"));
    }

    @Test
    void laExcepcionResuelveTodosSusParametros() {
        CargaUYException excepcion = new CargaUYException(CodigoError.PERMISO_SUPERPUESTO,
                "PNC-1001", "2024-01-01", "2025-12-31");

        String mensaje = MensajesError.resolver(excepcion);

        assertTrue(mensaje.contains("PNC-1001"));
        assertTrue(mensaje.contains("2024-01-01"));
        assertTrue(mensaje.contains("2025-12-31"));
        assertFalse(mensaje.contains("{"));
    }
}
