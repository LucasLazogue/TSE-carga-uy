package tse.cargauy.web.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import tse.cargauy.dtos.TokenDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.negocio.usuario.UsuarioEJBLocal;

class LoginViewTest {

    private final UsuarioEJBLocal usuarios = mock(UsuarioEJBLocal.class);
    private final HttpServletResponse respuesta = mock(HttpServletResponse.class);
    private LoginView login;

    @BeforeEach
    void preparar() throws ReflectiveOperationException {
        FacesContext contexto = ContextoFaces.usar(rol -> false);
        when(contexto.getExternalContext().getResponse()).thenReturn(respuesta);
        login = new LoginView();
        Field ejb = LoginView.class.getDeclaredField("usuarioEJB");
        ejb.setAccessible(true);
        ejb.set(login, usuarios);
        login.setUsuario("ana");
        login.setClave("clave");
        when(usuarios.crearToken(any())).thenReturn(new TokenDto("jwt-de-prueba", 3600));
    }

    @AfterEach
    void soltar() {
        ContextoFaces.soltar();
    }

    @Test
    void elAdministradorEntraANodosConElTokenEnUnaCookie() {
        when(usuarios.loginBackoffice("ana", "clave")).thenReturn(new UsuarioDto("ana", List.of("ADMINISTRADOR")));

        assertEquals("/admin/nodos?faces-redirect=true", login.ingresar());

        ArgumentCaptor<Cookie> cookie = ArgumentCaptor.forClass(Cookie.class);
        verify(respuesta).addCookie(cookie.capture());
        assertEquals("cargauy_token", cookie.getValue().getName());
        assertEquals("jwt-de-prueba", cookie.getValue().getValue());
        assertTrue(cookie.getValue().isHttpOnly());
    }

    @Test
    void laAutoridadEntraAReportes() {
        when(usuarios.loginBackoffice("ana", "clave")).thenReturn(new UsuarioDto("ana", List.of("AUTORIDAD")));

        assertEquals("/autoridad/reportes?faces-redirect=true", login.ingresar());
    }
}
