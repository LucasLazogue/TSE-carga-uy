package tse.cargauy.adaptadores.gubuy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

// el flujo de codigo de autorizacion contra un proveedor OIDC local que hace de gub.uy
class GubUyClientFlujoTest {

    private static final String CLIENTE = "890192";
    private static final String NONCE = "abc";
    private static final String RETORNO = "http://localhost:8080/api/auth/callback";

    private final GubUyClient cliente = new GubUyClient();
    private final Map<String, Respuesta> respuestas = new ConcurrentHashMap<>();
    private final List<String> pedidos = new ArrayList<>();

    private HttpServer servidor;
    private String url;

    private record Respuesta(int estado, String cuerpo) {
    }

    @BeforeEach
    void setUp() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", this::atender);
        servidor.start();
        url = "http://localhost:" + servidor.getAddress().getPort() + "/oidc/v1";
        configurar(url, CLIENTE, "secreto", false);
    }

    @AfterEach
    void tearDown() {
        servidor.stop(0);
    }

    private void atender(HttpExchange pedido) throws IOException {
        String ruta = pedido.getRequestURI().getPath();
        synchronized (pedidos) {
            pedidos.add(pedido.getRequestMethod() + " " + ruta + " " + pedido.getRequestHeaders().getFirst("Authorization"));
        }
        Respuesta respuesta = respuestas.getOrDefault(ruta.substring(ruta.lastIndexOf('/') + 1), new Respuesta(404, "{}"));
        byte[] cuerpo = respuesta.cuerpo().getBytes(StandardCharsets.UTF_8);
        pedido.sendResponseHeaders(respuesta.estado(), cuerpo.length);
        try (OutputStream salida = pedido.getResponseBody()) {
            salida.write(cuerpo);
        }
    }

    private void configurar(String url, String clientId, String secreto, boolean mock) throws Exception {
        poner("url", url);
        poner("clientId", clientId);
        poner("clientSecret", secreto);
        poner("redirectUri", RETORNO);
        poner("mock", mock);
    }

    private void poner(String campo, Object valor) throws Exception {
        Field f = GubUyClient.class.getDeclaredField(campo);
        f.setAccessible(true);
        f.set(cliente, valor);
    }

    private String idToken(String sub) {
        String payload = "{\"iss\":\"" + url + "\",\"aud\":\"" + CLIENTE + "\",\"sub\":\"" + sub + "\",\"nonce\":\"" + NONCE
                + "\",\"exp\":" + Instant.now().plusSeconds(300).getEpochSecond() + "}";
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        return b64.encodeToString("{}".getBytes(StandardCharsets.UTF_8)) + "."
                + b64.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".firma";
    }

    private void tokens(String sub) {
        respuestas.put("token", new Respuesta(200,
                "{\"access_token\":\"at-1\",\"id_token\":\"" + idToken(sub) + "\"}"));
    }

    private static void assertIdentidad(String cedula, String correo, IdentidadGubUy identidad) {
        assertEquals(cedula, identidad.getCedula());
        assertEquals(correo, identidad.getCorreo());
    }

    private void assertCodigo(CodigoError esperado, Runnable accion) {
        CargaUYException e = assertThrows(CargaUYException.class, accion::run);
        assertEquals(esperado, e.getCodigo());
    }

    @Test
    void urlDeAutorizacionConLosParametrosCodificados() {
        URI uri = cliente.getAuthorizationUrl("es tado", NONCE, null);

        String texto = uri.toString();
        assertTrue(texto.startsWith(url + "/authorize?response_type=code"));
        assertTrue(texto.contains("&client_id=" + CLIENTE));
        assertTrue(texto.contains("&redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fapi%2Fauth%2Fcallback"));
        assertTrue(texto.contains("&scope=openid+personal_info+document+email"));
        assertTrue(texto.contains("&state=es+tado"));
        assertTrue(texto.contains("&nonce=" + NONCE));
        assertFalse(cliente.isMock());
    }

    @Test
    void sinClienteConfiguradoNoArmaLaUrl() throws Exception {
        configurar(url, null, "secreto", false);
        assertCodigo(CodigoError.AUTH_CLIENTE_NO_CONFIGURADO, () -> cliente.getAuthorizationUrl("s", NONCE, null));

        configurar(url, CLIENTE, " ", false);
        assertCodigo(CodigoError.AUTH_CLIENTE_NO_CONFIGURADO, () -> cliente.exchangeCode("c", NONCE));
        assertTrue(pedidos.isEmpty());
    }

    @Test
    void canjeaElCodigoYDevuelveLaIdentidad() {
        tokens("5869");
        respuestas.put("userinfo", new Respuesta(200,
                "{\"sub\":\"5869\",\"numero_documento\":\"11111111\",\"email\":\"ana@correo.uy\"}"));

        IdentidadGubUy identidad = cliente.exchangeCode("codigo-1", NONCE);

        assertIdentidad("11111111", "ana@correo.uy", identidad);
        String basic = "Basic " + Base64.getEncoder().encodeToString((CLIENTE + ":secreto").getBytes(StandardCharsets.UTF_8));
        assertEquals(List.of("POST /oidc/v1/token " + basic, "GET /oidc/v1/userinfo Bearer at-1"), pedidos);
    }

    // los datos del usuario tienen que ser del mismo sujeto que el id_token
    @Test
    void rechazaDatosDeUsuarioDeOtroSujeto() {
        tokens("5869");
        respuestas.put("userinfo", new Respuesta(200, "{\"sub\":\"9999\",\"numero_documento\":\"22222222\"}"));

        assertCodigo(CodigoError.AUTH_TOKEN_INVALIDO, () -> cliente.exchangeCode("codigo-1", NONCE));
    }

    @Test
    void rechazaUnIdTokenConOtroNonce() {
        tokens("5869");

        assertCodigo(CodigoError.AUTH_TOKEN_INVALIDO, () -> cliente.exchangeCode("codigo-1", "otro"));
        assertEquals(1, pedidos.size(), "no pide los datos del usuario");
    }

    @Test
    void unCodigoRechazadoPorElProveedor() {
        respuestas.put("token", new Respuesta(400, "{\"error\":\"invalid_grant\"}"));

        assertCodigo(CodigoError.AUTH_TOKEN_INVALIDO, () -> cliente.exchangeCode("usado", NONCE));
    }

    @Test
    void unaRespuestaQueNoEsJson() {
        respuestas.put("token", new Respuesta(200, "<html>mantenimiento</html>"));

        assertCodigo(CodigoError.AUTH_TOKEN_INVALIDO, () -> cliente.exchangeCode("codigo-1", NONCE));
    }

    @Test
    void sinIdTokenEnLaRespuesta() {
        respuestas.put("token", new Respuesta(200, "{\"access_token\":\"at-1\"}"));

        assertCodigo(CodigoError.AUTH_TOKEN_INVALIDO, () -> cliente.exchangeCode("codigo-1", NONCE));
    }

    @Test
    void elProveedorNoResponde() throws Exception {
        int puertoLibre;
        try (ServerSocket socket = new ServerSocket(0)) {
            puertoLibre = socket.getLocalPort();
        }
        configurar("http://localhost:" + puertoLibre + "/oidc/v1", CLIENTE, "secreto", false);

        assertCodigo(CodigoError.AUTH_PROVEEDOR_NO_DISPONIBLE, () -> cliente.exchangeCode("codigo-1", NONCE));
    }

    @Test
    void enModoSimuladoNoLlamaAlProveedor() throws Exception {
        configurar(url, null, null, true);

        assertTrue(cliente.isMock());
        assertEquals(URI.create(RETORNO + "?code=55555555&state=s"), cliente.getAuthorizationUrl("s", NONCE, " "));
        assertEquals(URI.create(RETORNO + "?code=12345678&state=s"), cliente.getAuthorizationUrl("s", NONCE, "12345678"));
        assertIdentidad("12345678", "12345678@mock.uy", cliente.exchangeCode("12345678", NONCE));
        assertTrue(pedidos.isEmpty());
    }

    // init lee la configuracion del entorno; en los tests no esta definida
    @Test
    void initSinVariablesDeEntornoQuedaSinConfigurar() {
        assumeTrue(System.getenv("GUBUY_CLIENT_ID") == null && System.getenv("GUBUY_MOCK") == null);
        GubUyClient nuevo = new GubUyClient();
        nuevo.init();

        assertFalse(nuevo.isMock());
        assertCodigo(CodigoError.AUTH_CLIENTE_NO_CONFIGURADO, () -> nuevo.getAuthorizationUrl("s", NONCE, null));
    }
}
