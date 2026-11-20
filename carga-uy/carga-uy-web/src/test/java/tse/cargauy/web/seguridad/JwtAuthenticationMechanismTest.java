package tse.cargauy.web.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.entities.Rol;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.usuario.UsuarioEJBLocal;
import tse.cargauy.web.ws.EjbFalso;

// el token de sesion llega en el header Authorization (movil) o en la cookie (web) y se le pasa al contenedor el
// usuario con sus roles, que es lo que despues mira @RolesAllowed en los EJB (R9)
class JwtAuthenticationMechanismTest {

    private final JwtAuthenticationMechanism mecanismo = new JwtAuthenticationMechanism();
    private final EjbFalso<UsuarioEJBLocal> usuarios = EjbFalso.de(UsuarioEJBLocal.class);
    private final HttpServletRequest pedido = mock(HttpServletRequest.class);
    private final HttpServletResponse respuesta = mock(HttpServletResponse.class);
    private final HttpMessageContext contexto = mock(HttpMessageContext.class);

    @BeforeEach
    void preparar() {
        mecanismo.usuarioEJB = usuarios.ejb();
        UsuarioDto usuario = new UsuarioDto();
        usuario.setId(5L);
        usuario.setRoles(List.of(Rol.RESPONSABLE));
        usuarios.responde("validarToken", args -> {
            if (!"jwt-valido".equals(args[0])) {
                throw new CargaUYException(CodigoError.AUTH_SESION_INVALIDA);
            }
            return usuario;
        });
        when(contexto.notifyContainerAboutLogin(anyString(), any())).thenReturn(AuthenticationStatus.SUCCESS);
        when(contexto.doNothing()).thenReturn(AuthenticationStatus.NOT_DONE);
        when(contexto.redirect(anyString())).thenReturn(AuthenticationStatus.SEND_CONTINUE);
        when(pedido.getContextPath()).thenReturn("/carga-uy");
    }

    @Test
    void conBearerAutenticaConLosRolesDelUsuario() {
        when(pedido.getHeader("Authorization")).thenReturn("Bearer jwt-valido");

        assertEquals(AuthenticationStatus.SUCCESS, mecanismo.validateRequest(pedido, respuesta, contexto));
        verify(contexto).notifyContainerAboutLogin("5", Set.of(Rol.RESPONSABLE));
    }

    @Test
    void conLaCookieDeSesionTambien() {
        when(pedido.getCookies()).thenReturn(new Cookie[] { new Cookie("otra", "x"), new Cookie("cargauy_token", "jwt-valido") });

        assertEquals(AuthenticationStatus.SUCCESS, mecanismo.validateRequest(pedido, respuesta, contexto));
    }

    // un recurso publico sigue sin usuario; uno protegido manda al login
    @Test
    void sinTokenNoAutentica() {
        assertEquals(AuthenticationStatus.NOT_DONE, mecanismo.validateRequest(pedido, respuesta, contexto));

        when(contexto.isProtected()).thenReturn(true);
        assertEquals(AuthenticationStatus.SEND_CONTINUE, mecanismo.validateRequest(pedido, respuesta, contexto));
        verify(contexto).redirect("/carga-uy/login.xhtml");
        assertTrue(usuarios.llamadas().isEmpty());
    }

    @Test
    void unTokenInvalidoONoBearerNoAutentica() {
        when(pedido.getHeader("Authorization")).thenReturn("Bearer jwt-vencido");
        assertEquals(AuthenticationStatus.NOT_DONE, mecanismo.validateRequest(pedido, respuesta, contexto));

        when(pedido.getHeader("Authorization")).thenReturn("Basic dXNlcjpjbGF2ZQ==");
        when(pedido.getCookies()).thenReturn(new Cookie[] { new Cookie("otra", "x") });
        assertEquals(AuthenticationStatus.NOT_DONE, mecanismo.validateRequest(pedido, respuesta, contexto));

        when(pedido.getCookies()).thenReturn(new Cookie[] { new Cookie("cargauy_token", " ") });
        assertEquals(AuthenticationStatus.NOT_DONE, mecanismo.validateRequest(pedido, respuesta, contexto));

        verify(contexto, never()).notifyContainerAboutLogin(anyString(), any());
    }
}
