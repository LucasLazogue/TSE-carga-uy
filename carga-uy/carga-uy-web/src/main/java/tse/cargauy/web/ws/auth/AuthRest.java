package tse.cargauy.web.ws.auth;

import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
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
import tse.cargauy.dtos.TokenDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.usuario.UsuarioEJBLocal;

@RequestScoped
@Path("/auth")
public class AuthRest {

    private static final String STATE = "gubuy_state";
    private static final String NONCE = "gubuy_nonce";
    private static final String CODE_CHALLENGE = "mobile_code_challenge";
    private static final String MOBILE = "mobile";
    private static final String BEARER = "Bearer ";
    private static final String GRANT_TYPE = "authorization_code";
    private static final String COOKIE = "cargauy_token";
    private static final URI FRONTEND = URI.create(System.getenv("CARGAUY_FRONTEND_URL")).resolve("/");
    private static final URI APP_MOBILE = URI.create(System.getenv().getOrDefault("CARGAUY_MOBILE_REDIRECT", "cargauy://ingreso"));
    private static final SecureRandom RANDOM = new SecureRandom();

    @EJB
    UsuarioEJBLocal usuarioEJB;

    @Context
    HttpServletRequest request;

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
            HttpSession session = request.getSession();
            session.setAttribute(STATE, state);
            session.setAttribute(NONCE, nonce);
            session.setAttribute(CODE_CHALLENGE, mobile ? codeChallenge : null);
            return Response.seeOther(destino).build();
        } catch (CargaUYException e) {
            return redirectWithError(mobile, e);
        }
    }

    @GET
    @Path("/callback")
    public Response callback(@QueryParam("code") String code, @QueryParam("state") String state) {
        HttpSession session = request.getSession(false);
        String codeChallenge = session == null ? null : (String) session.getAttribute(CODE_CHALLENGE);
        try {
            if (session == null || code == null || state == null || !state.equals(session.getAttribute(STATE))) {
                throw new CargaUYException(CodigoError.AUTH_SOLICITUD_INVALIDA);
            }
            UsuarioDto usuario = usuarioEJB.loginGubUy(code, (String) session.getAttribute(NONCE));
            session.invalidate();

            if (codeChallenge != null) {
                String codigo = usuarioEJB.crearCodigoMobile(usuario, codeChallenge);
                return Response.seeOther(URI.create(APP_MOBILE + "?code=" + codigo)).build();
            }
            return Response.seeOther(FRONTEND).cookie(cookieSesion(usuarioEJB.crearToken(usuario).getToken(), NewCookie.DEFAULT_MAX_AGE)).build();
        } catch (CargaUYException e) {
            return redirectWithError(codeChallenge != null, e);
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

    @POST
    @Path("/logout")
    public Response logout() {
        return Response.noContent().cookie(cookieSesion("", 0)).build();
    }

    private Response redirectWithError(boolean mobile, CargaUYException e) {
        String error = "?error=" + e.getCodigo().getCodigo();
        return Response.seeOther(URI.create((mobile ? APP_MOBILE : FRONTEND) + error)).build();
    }

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
