package tse.cargauy.web.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.security.Principal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import jakarta.ejb.EJBAccessException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

class MapeoErroresTest {

    @ParameterizedTest
    @EnumSource(CodigoError.class)
    void cadaCodigoUsaElStatusDeSuCategoria(CodigoError codigo) {
        Response respuesta = new CargaUYExceptionMapper().toResponse(new CargaUYException(codigo));

        assertEquals(codigo.getCategoria().getStatus(), respuesta.getStatus());
        assertEquals(codigo.getCodigo(), ((ErrorDto) respuesta.getEntity()).getCodigo());
    }

    @Test
    void sinSesionDevuelve401() {
        AccesoDenegadoMapper mapper = new AccesoDenegadoMapper();
        mapper.seguridad = seguridad(null);

        Response respuesta = mapper.toResponse(new EJBAccessException());

        assertEquals(401, respuesta.getStatus());
        assertEquals(CodigoError.AUTH_SESION_INVALIDA.getCodigo(), ((ErrorDto) respuesta.getEntity()).getCodigo());
    }

    @Test
    void conSesionPeroSinRolDevuelve403() {
        AccesoDenegadoMapper mapper = new AccesoDenegadoMapper();
        mapper.seguridad = seguridad(() -> "22222222");

        Response respuesta = mapper.toResponse(new EJBAccessException());

        assertEquals(403, respuesta.getStatus());
        assertEquals(CodigoError.ACCESO_DENEGADO.getCodigo(), ((ErrorDto) respuesta.getEntity()).getCodigo());
    }

    @Test
    void errorInesperadoDevuelve500SinDetalles() {
        Response respuesta = new ErrorInesperadoMapper().toResponse(new IllegalStateException("detalle interno"));

        assertEquals(500, respuesta.getStatus());
        assertEquals("Ocurrio un error inesperado.", ((ErrorDto) respuesta.getEntity()).getMensaje());
    }

    @Test
    void errorDeJaxRsConservaSuRespuesta() {
        NotFoundException excepcion = new NotFoundException();

        Response respuesta = new ErrorInesperadoMapper().toResponse(excepcion);

        assertSame(excepcion.getResponse(), respuesta);
    }

    private static SecurityContext seguridad(Principal usuario) {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return usuario;
            }

            @Override
            public boolean isUserInRole(String rol) {
                return false;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        };
    }
}
