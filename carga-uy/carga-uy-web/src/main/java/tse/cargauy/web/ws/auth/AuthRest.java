package tse.cargauy.web.ws.auth;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.exceptions.MensajesError;
import tse.cargauy.negocio.usuario.UsuarioEJBLocal;

@RequestScoped
@Path("/auth")
public class AuthRest {

    // mientras no haya credenciales de ID Uruguay, /login se saltea gub.uy y devuelve el token directo
    private static final boolean MOCK = "true".equals(System.getenv("GUBUY_MOCK"));
    // el chofer que siembra DatosPrueba, asi el login sin cedula tambien sirve para la app
    private static final String CEDULA_PRUEBA = "55555555";
    private static final String STATE = "gubuy_state";
    private static final String NONCE = "gubuy_nonce";
    private static final String CLIENTE = "gubuy_cliente";
    private static final String MOBILE = "mobile";
    private static final String BEARER = "Bearer ";
    private static final String COOKIE = "cargauy_token";
    // a donde vuelve el usuario al terminar el login; solo estos dos, para no redirigir a cualquier url del pedido
    private static final URI FRONTEND = URI.create(System.getenv("CARGAUY_FRONTEND_URL")).resolve("/");
    private static final URI APP_MOBILE = URI.create(System.getenv("CARGAUY_MOBILE_REDIRECT"));
    private static final SecureRandom RANDOM = new SecureRandom();

    @EJB
    UsuarioEJBLocal usuarioEJB;

    @Context
    HttpServletRequest request;

    // cliente: web (por defecto) o mobile. cedula: solo con GUBUY_MOCK, para entrar como otro usuario de prueba
    @GET
    @Path("/login")
    public Response login(@QueryParam("cliente") String cliente, @QueryParam("cedula") String cedula) {
        if (MOCK) {
            try {
                UsuarioDto usuario = usuarioEJB.loginPrueba(cedula == null ? CEDULA_PRUEBA : cedula);
                return redirectWithToken(cliente, usuarioEJB.crearToken(usuario));
            } catch (CargaUYException e) {
                return redirectWithError(cliente, e);
            }
        }

        String state = randomValue();
        String nonce = randomValue();
        try {
            URI destino = usuarioEJB.getGubUyLoginUrl(state, nonce);
            HttpSession session = request.getSession();
            session.setAttribute(STATE, state);
            session.setAttribute(NONCE, nonce);
            session.setAttribute(CLIENTE, cliente);
            return Response.seeOther(destino).build();
        } catch (CargaUYException e) {
            return redirectWithError(cliente, e);
        }
    }

    @GET
    @Path("/callback")
    public Response callback(@QueryParam("code") String code, @QueryParam("state") String state) {
        HttpSession session = request.getSession(false);
        String cliente = session == null ? null : (String) session.getAttribute(CLIENTE);
        try {
            if (session == null || code == null || state == null || !state.equals(session.getAttribute(STATE))) {
                throw new CargaUYException(CodigoError.AUTH_SOLICITUD_INVALIDA);
            }
            UsuarioDto usuario = usuarioEJB.loginGubUy(code, (String) session.getAttribute(NONCE));

            // la sesion http solo sirve para el ida y vuelta con gub.uy, de aca en mas se usa el token
            session.invalidate();
            return redirectWithToken(cliente, usuarioEJB.crearToken(usuario));
        } catch (CargaUYException e) {
            return redirectWithError(cliente, e);
        }
    }

    // mobile manda el token como Bearer; el navegador lo manda solo, en la cookie
    @GET
    @Path("/me")
    @Produces("application/json")
    public UsuarioDto me(@HeaderParam("Authorization") String authorization, @CookieParam(COOKIE) String cookie) {
        String token = authorization != null && authorization.startsWith(BEARER)
                ? authorization.substring(BEARER.length())
                : cookie;
        if (token == null) {
            throw new NotAuthorizedException("Bearer");
        }
        return usuarioEJB.validarToken(token);
    }

    // el javascript del front no puede borrar una cookie HttpOnly, la tiene que borrar el backend
    @POST
    @Path("/logout")
    public Response logout() {
        return Response.noContent().cookie(cookieSesion("", 0)).build();
    }

    private static URI destino(String cliente) {
        return MOBILE.equals(cliente) ? APP_MOBILE : FRONTEND;
    }

    // la app recibe el token en el deep link y despues lo manda como Bearer. en el navegador va en una cookie
    // HttpOnly: el javascript de la pagina no la puede leer, asi que un XSS no se puede llevar el token
    private static Response redirectWithToken(String cliente, String token) {
        if (MOBILE.equals(cliente)) {
            return Response.seeOther(URI.create(APP_MOBILE + "#token=" + token)).build();
        }
        return Response.seeOther(FRONTEND).cookie(cookieSesion(token, NewCookie.DEFAULT_MAX_AGE)).build();
    }

    private static Response redirectWithError(String cliente, CargaUYException e) {
        String mensaje = URLEncoder.encode(MensajesError.resolver(e), StandardCharsets.UTF_8);
        return Response.seeOther(URI.create(destino(cliente) + "?error=" + mensaje)).build();
    }

    // path "/" porque el front llega por /api (proxy de vite) y el backend esta en /carga-uy/api.
    // SameSite=Lax: otro sitio no puede hacer que el navegador la mande en un POST (CSRF)
    private static NewCookie cookieSesion(String valor, int maxAge) {
        return new NewCookie.Builder(COOKIE)
                .value(valor)
                .path("/")
                .httpOnly(true)
                .secure("https".equals(FRONTEND.getScheme()))
                .sameSite(NewCookie.SameSite.LAX)
                .maxAge(maxAge)
                .build();
    }

    private static String randomValue() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
