package tse.cargauy.web.ws.auth;

import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import tse.cargauy.dtos.EstadoLoginDto;
import tse.cargauy.dtos.TokenDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.usuario.UsuarioEJBLocal;

@RequestScoped
@Path("/auth")
public class AuthRest {

    private static final String MOBILE = "mobile";
    private static final String GRANT_TYPE = "authorization_code";
    private static final String COOKIE_SESION = "cargauy_token";
    private static final String COOKIE_LOGIN = "cargauy_login";
    private static final URI FRONTEND = URI.create(System.getenv("CARGAUY_FRONTEND_URL")).resolve("/");
    private static final URI APP_MOBILE = URI.create(System.getenv().getOrDefault("CARGAUY_MOBILE_REDIRECT", "cargauy://ingreso"));
    private static final SecureRandom RANDOM = new SecureRandom();

    @EJB
    UsuarioEJBLocal usuarioEJB;

    @GET
    @Path("/login")
    public Response login(@QueryParam("cliente") String cliente, @QueryParam("code_challenge") String codeChallenge,
            @QueryParam("cedula") String cedulaMock) { // TODO mock: sacar cedula
        boolean mobile = MOBILE.equals(cliente);
        String state = randomValue();
        String nonce = randomValue();
        try {
            if (mobile && (codeChallenge == null || codeChallenge.isBlank())) {
                throw new CargaUYException(CodigoError.AUTH_SOLICITUD_INVALIDA);
            }
            URI destino = usuarioEJB.getGubUyLoginUrl(state, nonce, cedulaMock);
            TokenDto estado = usuarioEJB.crearEstadoLogin(state, nonce, mobile ? codeChallenge : null);
            return Response.seeOther(destino)
                    .cookie(cookie(COOKIE_LOGIN, estado.getToken(), (int) estado.getSegundosVigencia()))
                    .build();
        } catch (CargaUYException e) {
            return redirectWithError(mobile, e);
        }
    }

    @GET
    @Path("/callback")
    public Response callback(@QueryParam("code") String code, @QueryParam("state") String state,
            @CookieParam(COOKIE_LOGIN) String cookieLogin) {
        NewCookie borrarLogin = cookie(COOKIE_LOGIN, "", 0);
        String codeChallenge = null;
        try {
            EstadoLoginDto estado = usuarioEJB.leerEstadoLogin(cookieLogin);
            codeChallenge = estado.getCodeChallenge();
            if (code == null || state == null || !state.equals(estado.getState())) {
                throw new CargaUYException(CodigoError.AUTH_SOLICITUD_INVALIDA);
            }
            UsuarioDto usuario = usuarioEJB.loginGubUy(code, estado.getNonce());

            if (codeChallenge != null) {
                String codigo = usuarioEJB.crearCodigoMobile(usuario, codeChallenge);
                return Response.seeOther(URI.create(APP_MOBILE + "?code=" + codigo)).cookie(borrarLogin).build();
            }
            NewCookie sesion = cookie(COOKIE_SESION, usuarioEJB.crearToken(usuario).getToken(), NewCookie.DEFAULT_MAX_AGE);
            return Response.seeOther(FRONTEND).cookie(borrarLogin, sesion).build();
        } catch (CargaUYException e) {
            return redirectWithError(codeChallenge != null, e, borrarLogin);
        }
    }

    @POST
    @Path("/token")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    public JsonObject token(@FormParam("grant_type") String grantType, @FormParam("code") String code,
            @FormParam("code_verifier") String codeVerifier) {
        if (!GRANT_TYPE.equals(grantType)) {
            throw new CargaUYException(CodigoError.AUTH_SOLICITUD_INVALIDA);
        }
        TokenDto token = usuarioEJB.canjearCodigoMobile(code, codeVerifier);
        return Json.createObjectBuilder()
                .add("access_token", token.getToken())
                .add("token_type", "Bearer")
                .add("expires_in", token.getSegundosVigencia())
                .build();
    }

    @GET
    @Path("/config")
    @Produces("application/json")
    public JsonObject config() { // TODO mock: borrar
        return Json.createObjectBuilder().add("mock", usuarioEJB.isLoginMock()).build();
    }

    @POST
    @Path("/logout")
    public Response logout() {
        return Response.noContent().cookie(cookie(COOKIE_SESION, "", 0)).build();
    }

    private Response redirectWithError(boolean mobile, CargaUYException e, NewCookie... cookies) {
        String error = "?error=" + e.getCodigo().getCodigo();
        return Response.seeOther(URI.create((mobile ? APP_MOBILE : FRONTEND) + error)).cookie(cookies).build();
    }

    // HttpOnly: el js del front no la lee; Lax: viaja en la vuelta de gub.uy (navegacion GET) pero no en POST de otros sitios
    private static NewCookie cookie(String nombre, String valor, int maxAge) {
        return new NewCookie.Builder(nombre)
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
