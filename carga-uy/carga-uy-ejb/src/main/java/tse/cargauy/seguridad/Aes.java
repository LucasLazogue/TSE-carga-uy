package tse.cargauy.seguridad;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// cifra con AES-GCM. la clave de 256 bits sale del sha-256 del secreto, y cada cifrado usa un iv nuevo al azar
// que se guarda adelante del texto cifrado: base64(iv + cifrado)
public final class Aes {

    private static final int LARGO_IV = 12;
    private static final int LARGO_TAG = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Aes() {
    }

    public static String cifrar(String texto, String secreto) {
        try {
            byte[] iv = new byte[LARGO_IV];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, clave(secreto), new GCMParameterSpec(LARGO_TAG, iv));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String descifrar(String cifrado, String secreto) {
        try {
            byte[] datos = Base64.getDecoder().decode(cifrado);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave(secreto), new GCMParameterSpec(LARGO_TAG, datos, 0, LARGO_IV));
            return new String(cipher.doFinal(datos, LARGO_IV, datos.length - LARGO_IV), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static SecretKeySpec clave(String secreto) throws GeneralSecurityException {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(secreto.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(hash, "AES");
    }
}
