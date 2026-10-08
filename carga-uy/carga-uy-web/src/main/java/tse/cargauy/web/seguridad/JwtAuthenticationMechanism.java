package tse.cargauy.web.seguridad;

import java.util.HashSet;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.negocio.usuario.UsuarioEJBLocal;

@ApplicationScoped
public class JwtAuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final String BEARER = "Bearer ";
    private static final String COOKIE_SESION = "cargauy_token";

    @EJB
    UsuarioEJBLocal usuarioEJB;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response,
            HttpMessageContext contexto) {
        String token = token(request);
        if (token == null || token.isBlank()) {
            return contexto.doNothing();
        }
        try {
            UsuarioDto usuario = usuarioEJB.validarToken(token);
            return contexto.notifyContainerAboutLogin(String.valueOf(usuario.getId()), new HashSet<>(usuario.getRoles()));
        } catch (CargaUYException e) {
            return contexto.doNothing();
        }
    }

    private static String token(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER)) {
            return authorization.substring(BEARER.length());
        }
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_SESION.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
