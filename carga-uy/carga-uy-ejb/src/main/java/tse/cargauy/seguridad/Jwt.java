package tse.cargauy.seguridad;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;

// jwt firmado con HS256: header.payload.firma, cada parte en base64url y la firma es un HMAC-SHA256 con el secreto
public final class Jwt {

    private static final String ALGORITMO = "HmacSHA256";
    private static final String HEADER = base64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private Jwt() {
    }

    public static String firmar(JsonObject claims, String secreto) {
        String contenido = HEADER + "." + base64(claims.toString().getBytes(StandardCharsets.UTF_8));
        return contenido + "." + base64(hmac(contenido, secreto));
    }

    // devuelve null si esta mal formado, la firma no coincide, no tiene exp o ya vencio
    public static JsonObject verificar(String jwt, String secreto) {
        String[] partes = jwt == null ? new String[0] : jwt.split("\\.");
        if (partes.length != 3) {
            return null;
        }
        // el alg del header se ignora a proposito: si se le hiciera caso, quien arma el token podria elegir "none"
        byte[] esperada = hmac(partes[0] + "." + partes[1], secreto);
        try {
            if (!MessageDigest.isEqual(esperada, Base64.getUrlDecoder().decode(partes[2]))) {
                return null;
            }
            String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
            JsonObject claims = Json.createReader(new StringReader(payload)).readObject();
            if (!(claims.get("exp") instanceof JsonNumber exp)
                    || !Instant.ofEpochSecond(exp.longValue()).isAfter(Instant.now())) {
                return null;
            }
            return claims;
        } catch (IllegalArgumentException | JsonException e) {
            return null;
        }
    }

    private static byte[] hmac(String contenido, String secreto) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), ALGORITMO));
            return mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            // HmacSHA256 viene en toda JVM
            throw new IllegalStateException(e);
        }
    }

    private static String base64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
