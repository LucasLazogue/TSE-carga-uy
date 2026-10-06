package tse.cargauy.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.WeakKeyException;

class JwtTest {

    private static final SecretKey CLAVE = Jwt.clave("secreto-de-prueba-de-al-menos-32-bytes");
    private static final String TIPO = "sesion";

    private String jwtValido() {
        return Jwt.firmar("5001", TIPO, Map.of("cedula", "55555555", "roles", List.of("CHOFER")), Duration.ofMinutes(1), CLAVE);
    }

    private static String base64(String texto) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void verificaLoQueFirma() {
        Claims claims = Jwt.verificar(jwtValido(), TIPO, CLAVE);

        assertEquals("5001", claims.getSubject());
        assertEquals("55555555", claims.get("cedula", String.class));
        assertEquals(Jwt.ISSUER, claims.getIssuer());
        assertEquals(List.of("CHOFER"), claims.get("roles", List.class));
    }

    @Test
    void firmaConHs256() {
        String header = new String(Base64.getUrlDecoder().decode(jwtValido().split("\\.")[0]), StandardCharsets.UTF_8);

        assertTrue(header.contains("\"alg\":\"HS256\""));
    }

    @Test
    void rechazaOtroSecreto() {
        assertNull(Jwt.verificar(jwtValido(), TIPO, Jwt.clave("otro-secreto-de-prueba-de-al-menos-32-bytes")));
    }

    @Test
    void rechazaPayloadAlterado() {
        String[] partes = jwtValido().split("\\.");
        String otroPayload = base64("{\"iss\":\"carga-uy\",\"sub\":\"9999\",\"typ\":\"sesion\",\"exp\":"
                + Instant.now().plusSeconds(60).getEpochSecond() + "}");

        assertNull(Jwt.verificar(partes[0] + "." + otroPayload + "." + partes[2], TIPO, CLAVE));
    }

    @Test
    void rechazaSinFirma() {
        // alg none: un token que cualquiera puede armar sin conocer el secreto
        String sinFirma = Jwts.builder().issuer(Jwt.ISSUER).subject("5001").claim("typ", TIPO)
                .expiration(Date.from(Instant.now().plusSeconds(60))).compact();

        assertNull(Jwt.verificar(sinFirma, TIPO, CLAVE));
    }

    @Test
    void rechazaOtroTipo() {
        String codigoMobile = Jwt.firmar("5001", "codigo_mobile", Map.of(), Duration.ofMinutes(1), CLAVE);

        assertNull(Jwt.verificar(codigoMobile, TIPO, CLAVE));
    }

    @Test
    void rechazaOtroEmisor() {
        String otroEmisor = Jwts.builder().issuer("otro").subject("5001").claim("typ", TIPO)
                .expiration(Date.from(Instant.now().plusSeconds(60))).signWith(CLAVE, Jwts.SIG.HS256).compact();

        assertNull(Jwt.verificar(otroEmisor, TIPO, CLAVE));
    }

    @Test
    void rechazaVencido() {
        assertNull(Jwt.verificar(Jwt.firmar("5001", TIPO, Map.of(), Duration.ofSeconds(-1), CLAVE), TIPO, CLAVE));
    }

    @Test
    void rechazaSinVencimiento() {
        String sinExp = Jwts.builder().issuer(Jwt.ISSUER).subject("5001").claim("typ", TIPO)
                .signWith(CLAVE, Jwts.SIG.HS256).compact();

        assertNull(Jwt.verificar(sinExp, TIPO, CLAVE));
    }

    @Test
    void rechazaMalFormado() {
        assertNull(Jwt.verificar("no-es-un-jwt", TIPO, CLAVE));
        assertNull(Jwt.verificar("a.b.c", TIPO, CLAVE));
        assertNull(Jwt.verificar(null, TIPO, CLAVE));
    }

    @Test
    void rechazaSecretoCorto() {
        assertThrows(WeakKeyException.class, () -> Jwt.clave("secreto-de-31-bytes-exactamente"));
    }
}
