package tse.cargauy.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

// SHA-256 en base64url sin relleno: el challenge de PKCE (RFC 7636 4.2) y el hash con que se guardan los refresh tokens
public final class Sha256 {

    private Sha256() {
    }

    public static String base64Url(String valor) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // toda JVM trae SHA-256
            throw new IllegalStateException(e);
        }
    }
}
