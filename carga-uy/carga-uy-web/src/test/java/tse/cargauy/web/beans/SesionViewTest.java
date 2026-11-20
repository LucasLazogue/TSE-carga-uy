package tse.cargauy.web.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.negocio.usuario.UsuarioEJBLocal;

class SesionViewTest {

    private final UsuarioEJBLocal usuarios = mock(UsuarioEJBLocal.class);
    private final HttpServletResponse respuesta = mock(HttpServletResponse.class);
    private final SesionView sesion = new SesionView();
    private ExternalContext externo;

    @BeforeEach
    void preparar() throws ReflectiveOperationException {
        FacesContext contexto = ContextoFaces.usar(rol -> false);
        externo = contexto.getExternalContext();
        when(externo.getResponse()).thenReturn(respuesta);
        Field ejb = SesionView.class.getDeclaredField("usuarioEJB");
        ejb.setAccessible(true);
        ejb.set(sesion, usuarios);
    }

    @AfterEach
    void soltar() {
        ContextoFaces.soltar();
    }

    @Test
    void elNombreDelUsuarioSePideUnaVez() {
        when(usuarios.getUsuarioSesion()).thenReturn(new UsuarioDto("ana", List.of("ADMINISTRADOR")));

        assertEquals("ana", sesion.getUsuario());
        assertEquals("ana", sesion.getUsuario());
        verify(usuarios, times(1)).getUsuarioSesion();
    }

    // salir borra la cookie del token (vence ya) e invalida la sesion
    @Test
    void salirBorraLaCookieYVaAlLogin() {
        when(externo.isSecure()).thenReturn(true);

        assertEquals("/login?faces-redirect=true", sesion.salir());

        ArgumentCaptor<Cookie> cookie = ArgumentCaptor.forClass(Cookie.class);
        verify(respuesta).addCookie(cookie.capture());
        assertEquals("cargauy_token", cookie.getValue().getName());
        assertEquals("", cookie.getValue().getValue());
        assertEquals(0, cookie.getValue().getMaxAge());
        assertTrue(cookie.getValue().isHttpOnly());
        assertTrue(cookie.getValue().getSecure());
        verify(externo).invalidateSession();
    }
}
