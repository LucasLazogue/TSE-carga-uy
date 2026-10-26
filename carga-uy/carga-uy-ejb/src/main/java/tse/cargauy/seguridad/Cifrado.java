package tse.cargauy.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.InsecureSecretKeyAccess;
import com.google.crypto.tink.RegistryConfiguration;
import com.google.crypto.tink.TinkProtoKeysetFormat;
import com.google.crypto.tink.aead.AeadConfig;

import jakarta.enterprise.context.ApplicationScoped;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

// cifrado simetrico con Google Tink (AEAD), para secretos que hay que poder recuperar (ej. la clave con la que el
// central consulta a un nodo). el keyset sale de CARGAUY_CLAVE_CIFRADO: el de tinkey en formato binario, en base64.
// el dato asociado no se cifra pero queda atado al texto cifrado: si no coincide al descifrar, falla
@ApplicationScoped
public class Cifrado {

    public String cifrar(String texto, String asociado) {
        try {
            byte[] cifrado = aead().encrypt(texto.getBytes(StandardCharsets.UTF_8), asociado.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(cifrado);
        } catch (GeneralSecurityException e) {
            throw new CargaUYException(CodigoError.CIFRADO_NO_CONFIGURADO);
        }
    }

    public String descifrar(String cifrado, String asociado) {
        try {
            byte[] texto = aead().decrypt(Base64.getDecoder().decode(cifrado), asociado.getBytes(StandardCharsets.UTF_8));
            return new String(texto, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new CargaUYException(CodigoError.CIFRADO_INVALIDO);
        }
    }

    // aparte para poder reemplazarlo en los tests
    protected String keyset() {
        return System.getenv("CARGAUY_CLAVE_CIFRADO");
    }

    private Aead aead() {
        String keyset = keyset();
        if (keyset == null || keyset.isBlank()) {
            throw new CargaUYException(CodigoError.CIFRADO_NO_CONFIGURADO);
        }
        try {
            AeadConfig.register();
            return TinkProtoKeysetFormat.parseKeyset(Base64.getDecoder().decode(keyset), InsecureSecretKeyAccess.get())
                    .getPrimitive(RegistryConfiguration.get(), Aead.class);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new CargaUYException(CodigoError.CIFRADO_NO_CONFIGURADO);
        }
    }
}
