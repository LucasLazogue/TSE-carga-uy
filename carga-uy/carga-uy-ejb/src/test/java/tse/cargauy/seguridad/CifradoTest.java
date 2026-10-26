package tse.cargauy.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.GeneralSecurityException;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.google.crypto.tink.InsecureSecretKeyAccess;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.TinkProtoKeysetFormat;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.PredefinedAeadParameters;

import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

class CifradoTest {

    private final Cifrado cifrado = conKeyset(nuevoKeyset());

    @Test
    void descifraLoQueCifra() {
        String cifrada = cifrado.cifrar("clave-central-balanzas-sur", "balanzas-sur");

        assertFalse(cifrada.contains("clave-central"));
        assertEquals("clave-central-balanzas-sur", cifrado.descifrar(cifrada, "balanzas-sur"));
    }

    @Test
    void cadaCifradoEsDistinto() {
        assertNotEquals(cifrado.cifrar("clave", "balanzas-sur"), cifrado.cifrar("clave", "balanzas-sur"));
    }

    @Test
    void conOtroDatoAsociadoNoSeDescifra() {
        String cifrada = cifrado.cifrar("clave", "balanzas-sur");

        assertError(CodigoError.CIFRADO_INVALIDO, () -> cifrado.descifrar(cifrada, "balanzas-norte"));
    }

    @Test
    void conOtroKeysetNoSeDescifra() {
        String cifrada = cifrado.cifrar("clave", "balanzas-sur");

        assertError(CodigoError.CIFRADO_INVALIDO, () -> conKeyset(nuevoKeyset()).descifrar(cifrada, "balanzas-sur"));
    }

    @Test
    void sinKeysetFalla() {
        assertError(CodigoError.CIFRADO_NO_CONFIGURADO, () -> conKeyset(null).cifrar("clave", "balanzas-sur"));
    }

    @Test
    void conUnKeysetInvalidoFalla() {
        assertError(CodigoError.CIFRADO_NO_CONFIGURADO, () -> conKeyset("esto-no-es-un-keyset").cifrar("clave", "balanzas-sur"));
    }

    static Cifrado conKeyset(String keyset) {
        return new Cifrado() {
            @Override
            protected String keyset() {
                return keyset;
            }
        };
    }

    static String nuevoKeyset() {
        try {
            AeadConfig.register();
            KeysetHandle handle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM);
            return Base64.getEncoder().encodeToString(TinkProtoKeysetFormat.serializeKeyset(handle, InsecureSecretKeyAccess.get()));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void assertError(CodigoError codigo, Executable accion) {
        assertEquals(codigo, assertThrows(CargaUYException.class, accion).getCodigo());
    }
}
