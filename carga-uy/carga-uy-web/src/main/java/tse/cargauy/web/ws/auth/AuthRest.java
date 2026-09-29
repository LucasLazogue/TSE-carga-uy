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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.exceptions.MensajesError;
import tse.cargauy.negocio.usuario.UsuarioEJBLocal;

@RequestScoped
@Path("/auth")
public class AuthRest {

    private static final String STATE = "gubuy_state";
    private static final String NONCE = "gubuy_nonce";
    private static final String USUARIO = "usuario";
    private static final URI FRONTEND = URI.create(System.getenv("CARGAUY_FRONTEND_URL"));
    private static final SecureRandom RANDOM = new SecureRandom();

    @EJB
    UsuarioEJBLocal usuarioEJB;

    @Context
    HttpServletRequest request;

    @GET
    @Path("/login")
    public Response login() {
        String state = randomValue();
        String nonce = randomValue();
        try {
            URI destino = usuarioEJB.getGubUyLoginUrl(state, nonce);
            HttpSession session = request.getSession();
            session.setAttribute(STATE, state);
            session.setAttribute(NONCE, nonce);
            return Response.seeOther(destino).build();
        } catch (CargaUYException e) {
            return redirectWithError(e);
        }
    }

    @GET
    @Path("/callback")
    public Response callback(@QueryParam("code") String code, @QueryParam("state") String state) {
        HttpSession session = request.getSession(false);
        try {
            if (session == null || code == null || state == null || !state.equals(session.getAttribute(STATE))) {
                throw new CargaUYException(CodigoError.AUTH_SOLICITUD_INVALIDA);
            }
            UsuarioDto usuario = usuarioEJB.loginGubUy(code, (String) session.getAttribute(NONCE));

            session.removeAttribute(STATE);
            session.removeAttribute(NONCE);
            request.changeSessionId();
            session.setAttribute(USUARIO, usuario);
            return Response.seeOther(FRONTEND).build();
        } catch (CargaUYException e) {
            return redirectWithError(e);
        }
    }

    @GET
    @Path("/logout")
    public Response logout() {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Response.seeOther(FRONTEND).build();
    }

    @GET
    @Path("/me")
    @Produces("application/json")
    public UsuarioDto me() {
        HttpSession session = request.getSession(false);
        UsuarioDto usuario = session == null ? null : (UsuarioDto) session.getAttribute(USUARIO);
        if (usuario == null) {
            throw new NotAuthorizedException("Session");
        }
        return usuario;
    }

    private Response redirectWithError(CargaUYException e) {
        String mensaje = URLEncoder.encode(MensajesError.resolver(e), StandardCharsets.UTF_8);
        return Response.seeOther(FRONTEND.resolve("/?error=" + mensaje)).build();
    }

    private static String randomValue() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
