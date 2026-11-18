package tse.cargauy.web.vistas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;

import javax.naming.NameNotFoundException;

import org.junit.jupiter.api.Test;

import tse.cargauy.web.vistas.ClaveVistas.Estado;

class ClaveVistasTest {

    private static String clave(int bytes) {
        return Base64.getEncoder().encodeToString(new byte[bytes]);
    }

    @Test
    void aceptaClavesAesEnBase64() {
        assertEquals(Estado.VALIDA, ClaveVistas.revisar(clave(16)));
        assertEquals(Estado.VALIDA, ClaveVistas.revisar(clave(24)));
        assertEquals(Estado.VALIDA, ClaveVistas.revisar(clave(32)));
    }

    @Test
    void sinValorFalta() {
        assertEquals(Estado.FALTA, ClaveVistas.revisar(null));
        assertEquals(Estado.FALTA, ClaveVistas.revisar(""));
        assertEquals(Estado.FALTA, ClaveVistas.revisar("  "));
    }

    @Test
    void rechazaLoQueMojarraNoPuedeUsar() {
        assertEquals(Estado.INVALIDA, ClaveVistas.revisar(clave(10)));
        assertEquals(Estado.INVALIDA, ClaveVistas.revisar("no-es-base64!"));
        // con salto de linea al final (secreto creado desde un archivo) Mojarra tampoco la decodifica
        assertEquals(Estado.INVALIDA, ClaveVistas.revisar(clave(32) + "\n"));
    }

    @Test
    void sinEntradaEnJndiSigueConClavePropia() {
        assertEquals(Estado.FALTA, ClaveVistas.revisarYAvisar(() -> {
            throw new NameNotFoundException(ClaveVistas.JNDI);
        }));
    }

    @Test
    void conClaveValidaArranca() {
        assertEquals(Estado.VALIDA, ClaveVistas.revisarYAvisar(() -> clave(32)));
    }

    @Test
    void conClaveInvalidaNoDespliegaNiMuestraLaClave() {
        String invalida = "clave-que-no-sirve";
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ClaveVistas.revisarYAvisar(() -> invalida));
        assertFalse(e.getMessage().contains(invalida));
    }

    @Test
    void alArrancarSinJndiNoFalla() {
        // fuera de wildfly no hay contexto inicial: se toma como clave faltante
        new ClaveVistas().contextInitialized(null);
    }
}
