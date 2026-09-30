package tse.cargauy.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import jakarta.json.Json;
import jakarta.json.JsonObject;

class JwtTest {

    private static final String SECRETO = "secreto-de-prueba";

    private JsonObject claims(String sub, Instant exp) {
        return Json.createObjectBuilder().add("sub", sub).add("exp", exp.getEpochSecond()).build();
    }

    private String jwtValido() {
        return Jwt.firmar(claims("5001", Instant.now().plusSeconds(60)), SECRETO);
    }

    @Test
    void verificaLoQueFirma() {
        assertEquals("5001", Jwt.verificar(jwtValido(), SECRETO).getString("sub"));
    }

    @Test
    void rechazaOtroSecreto() {
        assertNull(Jwt.verificar(jwtValido(), "otro-secreto"));
    }

    @Test
    void rechazaPayloadAlterado() {
        String[] partes = jwtValido().split("\\.");
        String otroPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                claims("9999", Instant.now().plusSeconds(60)).toString().getBytes(StandardCharsets.UTF_8));

        assertNull(Jwt.verificar(partes[0] + "." + otroPayload + "." + partes[2], SECRETO));
    }

    @Test
    void rechazaVencido() {
        assertNull(Jwt.verificar(Jwt.firmar(claims("5001", Instant.now().minusSeconds(1)), SECRETO), SECRETO));
    }

    @Test
    void rechazaSinVencimiento() {
        assertNull(Jwt.verificar(Jwt.firmar(Json.createObjectBuilder().add("sub", "5001").build(), SECRETO), SECRETO));
    }

    @Test
    void rechazaMalFormado() {
        assertNull(Jwt.verificar("no-es-un-jwt", SECRETO));
        assertNull(Jwt.verificar("a.b.c", SECRETO));
        assertNull(Jwt.verificar(null, SECRETO));
    }
}
