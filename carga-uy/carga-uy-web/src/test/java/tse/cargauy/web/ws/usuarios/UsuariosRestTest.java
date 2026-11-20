package tse.cargauy.web.ws.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.Principal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.ws.rs.core.SecurityContext;
import tse.cargauy.dtos.UsuarioActualDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.usuario.UsuarioEJBLocal;
import tse.cargauy.web.ws.EjbFalso;

class UsuariosRestTest {

    @Test
    void elUsuarioActualRequiereSesion() {
        UsuarioActualDto actual = new UsuarioActualDto();
        EjbFalso<UsuarioEJBLocal> ejb = EjbFalso.de(UsuarioEJBLocal.class).responde("getUsuarioActual", actual);
        UsuariosRest rest = new UsuariosRest();
        rest.usuarioEJB = ejb.ejb();
        SecurityContext conSesion = Mockito.mock(SecurityContext.class);
        Mockito.when(conSesion.getUserPrincipal()).thenReturn((Principal) () -> "5");
        SecurityContext sinSesion = Mockito.mock(SecurityContext.class);

        assertSame(actual, rest.getUsuarioActual(conSesion));
        CargaUYException e = assertThrows(CargaUYException.class, () -> rest.getUsuarioActual(sinSesion));
        assertEquals(CodigoError.AUTH_SESION_INVALIDA, e.getCodigo());
        assertEquals(List.of("getUsuarioActual"), ejb.llamadas());
    }
}
