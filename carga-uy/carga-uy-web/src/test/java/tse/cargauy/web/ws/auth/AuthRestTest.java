package tse.cargauy.web.ws.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import tse.cargauy.dtos.EstadoLoginDto;
import tse.cargauy.dtos.TokenDto;
import tse.cargauy.dtos.TokensMovilDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.sesion.SesionMovilEJBLocal;
import tse.cargauy.negocio.usuario.UsuarioEJBLocal;
import tse.cargauy.web.ws.EjbFalso;

// el login con gub.uy para la web y el movil, y los tokens del movil. CARGAUY_FRONTEND_URL la pone surefire (pom)
class AuthRestTest {

    private static final URI GUBUY = URI.create("https://auth-testing.iduruguay.gub.uy/oidc/v1/authorize?x=1");
    private static final String FRONTEND = "http://localhost:5173/";
    private static final String APP = "cargauy://ingreso";

    private final AuthRest rest = new AuthRest();
    private final EjbFalso<UsuarioEJBLocal> usuarios = EjbFalso.de(UsuarioEJBLocal.class);
    private final EjbFalso<SesionMovilEJBLocal> sesiones = EjbFalso.de(SesionMovilEJBLocal.class);
    private final UsuarioDto usuario = new UsuarioDto();

    @BeforeEach
    void preparar() {
        rest.usuarioEJB = usuarios.ejb();
        rest.sesionMovilEJB = sesiones.ejb();
        usuarios.responde("getGubUyLoginUrl", GUBUY)
                .responde("crearEstadoLogin", new TokenDto("estado-firmado", 600))
                .responde("loginGubUy", usuario)
                .responde("crearToken", new TokenDto("jwt-sesion", 3600))
                .responde("crearCodigoMobile", "codigo-1");
    }

    private static Map<String, NewCookie> cookies(Response respuesta) {
        return respuesta.getCookies();
    }

    private static int codigo(CodigoError error) {
        return error.getCodigo();
    }

    @Test
    void elLoginWebRedirigeAGubUyConElEstadoEnUnaCookie() {
        Response respuesta = rest.login(null, null, null);

        assertEquals(303, respuesta.getStatus());
        assertEquals(GUBUY, respuesta.getLocation());
        NewCookie login = cookies(respuesta).get("cargauy_login");
        assertEquals("estado-firmado", login.getValue());
        assertEquals(600, login.getMaxAge());
        assertTrue(login.isHttpOnly());
        assertEquals(NewCookie.SameSite.LAX, login.getSameSite());
        assertFalse(login.isSecure(), "el frontend de los tests es http");
        assertNull(usuarios.argumento("crearEstadoLogin", 2), "la web no usa PKCE");
    }

    // state y nonce al azar en cada login: no se pueden adivinar ni reutilizar
    @Test
    void cadaLoginUsaOtroStateYNonce() {
        rest.login(null, null, null);
        Object[] primero = usuarios.argumentos("getGubUyLoginUrl");
        rest.login(null, null, null);
        Object[] segundo = usuarios.argumentos("getGubUyLoginUrl");

        assertNotEquals(primero[0], segundo[0]);
        assertNotEquals(primero[1], segundo[1]);
        assertNotEquals(primero[0], primero[1]);
    }

    @Test
    void elLoginDelMovilGuardaElCodeChallenge() {
        rest.login("mobile", "challenge-1", null);

        assertEquals("challenge-1", usuarios.argumento("crearEstadoLogin", 2));
    }

    @Test
    void elMovilSinCodeChallengeVuelveALaAppConError() {
        Response respuesta = rest.login("mobile", " ", null);

        assertEquals(URI.create(APP + "?error=" + codigo(CodigoError.AUTH_SOLICITUD_INVALIDA)), respuesta.getLocation());
        assertFalse(usuarios.llamadas().contains("getGubUyLoginUrl"));
    }

    @Test
    void unErrorAlArmarElLoginVuelveAlFrontend() {
        usuarios.falla("getGubUyLoginUrl", new CargaUYException(CodigoError.AUTH_CLIENTE_NO_CONFIGURADO));

        Response respuesta = rest.login(null, null, null);

        assertEquals(URI.create(FRONTEND + "?error=" + codigo(CodigoError.AUTH_CLIENTE_NO_CONFIGURADO)), respuesta.getLocation());
    }

    @Test
    void laVueltaWebDejaLaSesionEnUnaCookieYBorraLaDelLogin() {
        usuarios.responde("leerEstadoLogin", new EstadoLoginDto("s-1", "n-1", null));

        Response respuesta = rest.callback("codigo-gubuy", "s-1", "estado-firmado");

        assertEquals(URI.create(FRONTEND), respuesta.getLocation());
        assertEquals("jwt-sesion", cookies(respuesta).get("cargauy_token").getValue());
        assertEquals(0, cookies(respuesta).get("cargauy_login").getMaxAge());
        assertEquals(List.of("codigo-gubuy", "n-1"), List.of(usuarios.argumentos("loginGubUy")));
    }

    @Test
    void laVueltaDelMovilLlevaUnCodigoALaApp() {
        usuarios.responde("leerEstadoLogin", new EstadoLoginDto("s-1", "n-1", "challenge-1"));

        Response respuesta = rest.callback("codigo-gubuy", "s-1", "estado-firmado");

        assertEquals(URI.create(APP + "?code=codigo-1"), respuesta.getLocation());
        assertEquals("challenge-1", usuarios.argumento("crearCodigoMobile", 1));
        assertFalse(cookies(respuesta).containsKey("cargauy_token"), "el movil no usa la cookie de sesion");
    }

    // proteccion CSRF del login: el state que vuelve tiene que ser el que se guardo en la cookie
    @Test
    void otroStateEsRechazado() {
        usuarios.responde("leerEstadoLogin", new EstadoLoginDto("s-1", "n-1", null));

        Response distinto = rest.callback("codigo-gubuy", "otro", "estado-firmado");
        Response sinCodigo = rest.callback(null, "s-1", "estado-firmado");

        String error = FRONTEND + "?error=" + codigo(CodigoError.AUTH_SOLICITUD_INVALIDA);
        assertEquals(URI.create(error), distinto.getLocation());
        assertEquals(URI.create(error), sinCodigo.getLocation());
        assertEquals(0, cookies(distinto).get("cargauy_login").getMaxAge());
        assertFalse(usuarios.llamadas().contains("loginGubUy"));
    }

    @Test
    void unErrorEnLaVueltaDelMovilVuelveALaApp() {
        usuarios.responde("leerEstadoLogin", new EstadoLoginDto("s-1", "n-1", "challenge-1"))
                .falla("loginGubUy", new CargaUYException(CodigoError.AUTH_TOKEN_INVALIDO));

        Response respuesta = rest.callback("codigo-gubuy", "s-1", "estado-firmado");

        assertEquals(URI.create(APP + "?error=" + codigo(CodigoError.AUTH_TOKEN_INVALIDO)), respuesta.getLocation());
    }

    @Test
    void sinCookieDeLoginVuelveAlFrontendConError() {
        usuarios.falla("leerEstadoLogin", new CargaUYException(CodigoError.AUTH_SOLICITUD_INVALIDA));

        Response respuesta = rest.callback("codigo-gubuy", "s-1", null);

        assertTrue(respuesta.getLocation().toString().startsWith(FRONTEND + "?error="));
    }

    private static HttpServletRequest pedido() {
        HttpServletRequest pedido = mock(HttpServletRequest.class);
        when(pedido.getMethod()).thenReturn("POST");
        when(pedido.getRequestURL()).thenReturn(new StringBuffer("http://10.0.2.2:8080/carga-uy/api/auth/token"));
        return pedido;
    }

    // la prueba DPoP se valida contra el metodo y la URL del pedido
    @Test
    void elCanjeDelCodigoDevuelveLosTokensDelMovil() {
        sesiones.responde("iniciar", new TokensMovilDto("at-1", 900, "rt-1", 2592000));

        JsonObject tokens = rest.token("authorization_code", "codigo-1", "verificador", null, "prueba-dpop", pedido());

        assertEquals("at-1", tokens.getString("access_token"));
        assertEquals("Bearer", tokens.getString("token_type"));
        assertEquals(900, tokens.getInt("expires_in"));
        assertEquals("rt-1", tokens.getString("refresh_token"));
        assertEquals(2592000, tokens.getInt("refresh_expires_in"));
        assertEquals(List.of("codigo-1", "verificador", "prueba-dpop", "POST", "http://10.0.2.2:8080/carga-uy/api/auth/token"),
                List.of(sesiones.argumentos("iniciar")));
    }

    // si la renovacion no rota el refresh token, la respuesta no lo incluye
    @Test
    void laRenovacionSinRefreshNuevo() {
        sesiones.responde("renovar", new TokensMovilDto("at-2", 900, null, 100));

        JsonObject tokens = rest.token("refresh_token", null, null, "rt-1", "prueba-dpop", pedido());

        assertEquals("at-2", tokens.getString("access_token"));
        assertFalse(tokens.containsKey("refresh_token"));
        assertEquals("rt-1", sesiones.argumento("renovar", 0));
    }

    @Test
    void otroGrantEsRechazado() {
        CargaUYException e = assertThrows(CargaUYException.class,
                () -> rest.token("password", null, null, null, null, pedido()));

        assertEquals(CodigoError.AUTH_SOLICITUD_INVALIDA, e.getCodigo());
        assertTrue(sesiones.llamadas().isEmpty());
    }

    // RFC 7009: responde 200 aunque el token no exista
    @Test
    void revocarSiempreResponde200() {
        assertEquals(200, rest.revocar("rt-1").getStatus());
        assertEquals("rt-1", sesiones.argumento("revocar", 0));
    }

    @Test
    void configYLogout() {
        usuarios.responde("isLoginMock", true);

        assertTrue(rest.config().getBoolean("mock"));
        Response logout = rest.logout();
        assertEquals(204, logout.getStatus());
        assertEquals(0, cookies(logout).get("cargauy_token").getMaxAge());
        assertEquals("", cookies(logout).get("cargauy_token").getValue());
    }
}
