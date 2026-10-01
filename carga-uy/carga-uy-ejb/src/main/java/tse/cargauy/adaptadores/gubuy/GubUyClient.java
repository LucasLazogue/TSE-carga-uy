package tse.cargauy.adaptadores.gubuy;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@ApplicationScoped
public class GubUyClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String SCOPE = "openid personal_info document email";
    private static final String CEDULA_MOCK = "55555555"; // TODO mock

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    private String url;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private boolean mock; // TODO mock

    @PostConstruct
    void init() {
        url = System.getenv("GUBUY_URL");
        clientId = System.getenv("GUBUY_CLIENT_ID");
        clientSecret = System.getenv("GUBUY_CLIENT_SECRET");
        redirectUri = System.getenv("GUBUY_REDIRECT_URI");
        mock = Boolean.parseBoolean(System.getenv("GUBUY_MOCK")); // TODO mock
    }

    public URI getAuthorizationUrl(String state, String nonce, String cedulaMock) { // TODO mock: sacar cedulaMock
        if (mock) { // TODO mock: borrar este if
            String cedula = cedulaMock == null || cedulaMock.isBlank() ? CEDULA_MOCK : cedulaMock;
            return URI.create(redirectUri + "?code=" + encode(cedula) + "&state=" + encode(state));
        }
        validateConfiguration();
        return URI.create(url + "/authorize"
                + "?response_type=code"
                + "&client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&scope=" + encode(SCOPE)
                + "&state=" + encode(state)
                + "&nonce=" + encode(nonce));
    }

    public IdentidadGubUy exchangeCode(String code, String nonce) {
        if (mock) { // TODO mock: borrar este if
            return new IdentidadGubUy(code, code + "@mock.uy");
        }
        validateConfiguration();
        String credenciales = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        JsonObject tokens = send(HttpRequest.newBuilder(URI.create(url + "/token"))
                .header("Authorization", "Basic " + credenciales)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=authorization_code"
                        + "&code=" + encode(code)
                        + "&redirect_uri=" + encode(redirectUri))));

        JsonObject idToken = readIdToken(tokens.getString("id_token", ""));
        validateIdToken(idToken, url, clientId, nonce, Instant.now());

        JsonObject userInfo = send(HttpRequest.newBuilder(URI.create(url + "/userinfo"))
                .header("Authorization", "Bearer " + tokens.getString("access_token", ""))
                .GET());
        if (!idToken.getString("sub", "").equals(userInfo.getString("sub", null))) {
            throw new CargaUYException(CodigoError.AUTH_TOKEN_INVALIDO);
        }
        return new IdentidadGubUy(userInfo.getString("numero_documento", null), userInfo.getString("email", null));
    }

    static JsonObject readIdToken(String idToken) {
        String[] partes = idToken.split("\\.");
        if (partes.length != 3) {
            throw new CargaUYException(CodigoError.AUTH_TOKEN_INVALIDO);
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
            return Json.createReader(new StringReader(payload)).readObject();
        } catch (IllegalArgumentException | JsonException e) {
            throw new CargaUYException(CodigoError.AUTH_TOKEN_INVALIDO);
        }
    }

    static void validateIdToken(JsonObject idToken, String issuer, String clientId, String nonce, Instant ahora) {
        JsonValue exp = idToken.get("exp");
        boolean valido = issuer.equals(idToken.getString("iss", null))
                && hasAudience(idToken.get("aud"), clientId)
                && nonce.equals(idToken.getString("nonce", null))
                && exp instanceof JsonNumber numero
                && Instant.ofEpochSecond(numero.longValue()).isAfter(ahora);
        if (!valido) {
            throw new CargaUYException(CodigoError.AUTH_TOKEN_INVALIDO);
        }
    }

    private static boolean hasAudience(JsonValue aud, String clientId) {
        if (aud instanceof JsonString texto) {
            return clientId.equals(texto.getString());
        }
        return aud != null && aud.getValueType() == JsonValue.ValueType.ARRAY
                && aud.asJsonArray().getValuesAs(JsonString.class).stream().anyMatch(a -> clientId.equals(a.getString()));
    }

    private JsonObject send(HttpRequest.Builder request) {
        try {
            HttpResponse<String> response = http.send(request.timeout(TIMEOUT).build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new CargaUYException(CodigoError.AUTH_TOKEN_INVALIDO);
            }
            return Json.createReader(new StringReader(response.body())).readObject();
        } catch (IOException e) {
            throw new CargaUYException(CodigoError.AUTH_PROVEEDOR_NO_DISPONIBLE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CargaUYException(CodigoError.AUTH_PROVEEDOR_NO_DISPONIBLE);
        } catch (JsonException e) {
            throw new CargaUYException(CodigoError.AUTH_TOKEN_INVALIDO);
        }
    }

    private void validateConfiguration() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new CargaUYException(CodigoError.AUTH_CLIENTE_NO_CONFIGURADO);
        }
    }

    private static String encode(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
