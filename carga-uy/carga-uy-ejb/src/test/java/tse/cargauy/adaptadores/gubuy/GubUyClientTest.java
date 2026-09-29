package tse.cargauy.adaptadores.gubuy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

class GubUyClientTest {

    private static final String ISSUER = "https://auth-testing.iduruguay.gub.uy/oidc/v1";
    private static final Instant AHORA = Instant.parse("2026-09-28T12:00:00Z");

    private JsonObjectBuilder idTokenValido() {
        return Json.createObjectBuilder()
                .add("iss", ISSUER)
                .add("aud", "890192")
                .add("sub", "5869")
                .add("nonce", "abc")
                .add("exp", AHORA.plusSeconds(60).getEpochSecond());
    }

    private void assertRechazado(JsonObject idToken) {
        CargaUYException e = assertThrows(CargaUYException.class,
                () -> GubUyClient.validateIdToken(idToken, ISSUER, "890192", "abc", AHORA));
        assertEquals(CodigoError.AUTH_TOKEN_INVALIDO, e.getCodigo());
    }

    @Test
    void aceptaIdTokenValido() {
        assertDoesNotThrow(() -> GubUyClient.validateIdToken(idTokenValido().build(), ISSUER, "890192", "abc", AHORA));
    }

    @Test
    void aceptaAudienciaComoLista() {
        JsonObject idToken = idTokenValido().add("aud", Json.createArrayBuilder().add("otro").add("890192")).build();
        assertDoesNotThrow(() -> GubUyClient.validateIdToken(idToken, ISSUER, "890192", "abc", AHORA));
    }

    @Test
    void rechazaOtroEmisor() {
        assertRechazado(idTokenValido().add("iss", "https://otro.uy").build());
    }

    @Test
    void rechazaOtraAudiencia() {
        assertRechazado(idTokenValido().add("aud", "123").build());
    }

    @Test
    void rechazaOtroNonce() {
        assertRechazado(idTokenValido().add("nonce", "xyz").build());
    }

    @Test
    void rechazaIdTokenVencido() {
        assertRechazado(idTokenValido().add("exp", AHORA.minusSeconds(1).getEpochSecond()).build());
    }

    @Test
    void leeElPayloadDelIdToken() {
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"5869\"}".getBytes(StandardCharsets.UTF_8));

        assertEquals("5869", GubUyClient.readIdToken("encabezado." + payload + ".firma").getString("sub"));
    }

    @Test
    void rechazaIdTokenMalFormado() {
        assertThrows(CargaUYException.class, () -> GubUyClient.readIdToken("no-es-un-jwt"));
    }
}
