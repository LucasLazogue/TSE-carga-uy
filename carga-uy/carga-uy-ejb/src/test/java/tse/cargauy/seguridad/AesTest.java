package tse.cargauy.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AesTest {

    private static final String SECRETO = "secreto-de-prueba-de-al-menos-32-bytes";

    @Test
    void descifraLoQueCifra() {
        String cifrado = Aes.cifrar("clave-central-balanzas-sur", SECRETO);

        assertFalse(cifrado.contains("clave-central"));
        assertEquals("clave-central-balanzas-sur", Aes.descifrar(cifrado, SECRETO));
    }

    @Test
    void cadaCifradoEsDistinto() {
        assertNotEquals(Aes.cifrar("clave", SECRETO), Aes.cifrar("clave", SECRETO));
    }

    @Test
    void conOtroSecretoNoSePuedeDescifrar() {
        String cifrado = Aes.cifrar("clave", SECRETO);

        assertThrows(IllegalStateException.class, () -> Aes.descifrar(cifrado, "otro-secreto-de-al-menos-32-bytes!"));
    }
}
