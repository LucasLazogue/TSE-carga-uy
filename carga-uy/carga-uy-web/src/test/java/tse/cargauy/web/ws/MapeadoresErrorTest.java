package tse.cargauy.web.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.junit.jupiter.api.Test;

import jakarta.ejb.EJBAccessException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

// cada error sale con su codigo HTTP y un cuerpo con el codigo de error, sin detalles internos
class MapeadoresErrorTest {

    private static ErrorDto error(Response respuesta) {
        return (ErrorDto) respuesta.getEntity();
    }

    @Test
    void unErrorDelNegocioSaleConSuCategoria() {
        Response respuesta = new CargaUYExceptionMapper().toResponse(new CargaUYException(CodigoError.GUIA_NO_ENCONTRADA, "3"));

        assertEquals(404, respuesta.getStatus());
        assertEquals(CodigoError.GUIA_NO_ENCONTRADA.getCodigo(), error(respuesta).getCodigo());
    }

    // R9: la autorizacion de la capa de servicios (@RolesAllowed) se ve como 401 sin sesion y 403 con otro rol
    @Test
    void unAccesoDenegadoPorRolEs401SinSesionY403ConSesion() {
        AccesoDenegadoMapper mapper = new AccesoDenegadoMapper();
        mapper.seguridad = mock(SecurityContext.class);

        Response sinSesion = mapper.toResponse(new EJBAccessException());
        when(mapper.seguridad.getUserPrincipal()).thenReturn((Principal) () -> "5");
        Response otroRol = mapper.toResponse(new EJBAccessException());

        assertEquals(401, sinSesion.getStatus());
        assertEquals(CodigoError.AUTH_SESION_INVALIDA.getCodigo(), error(sinSesion).getCodigo());
        assertEquals(403, otroRol.getStatus());
        assertEquals(CodigoError.ACCESO_DENEGADO.getCodigo(), error(otroRol).getCodigo());
    }

    // un error no previsto es un 500 generico: el detalle queda en el log, no en la respuesta
    @Test
    void unErrorInesperadoNoMuestraElDetalle() {
        Response respuesta = new ErrorInesperadoMapper().toResponse(new IllegalStateException("password=123 en la base"));

        assertEquals(500, respuesta.getStatus());
        assertEquals(500, error(respuesta).getCodigo());
        assertEquals("Ocurrio un error inesperado.", error(respuesta).getMensaje());
    }

    @Test
    void lasExcepcionesHttpConservanSuRespuesta() {
        NotFoundException noEncontrado = new NotFoundException();

        assertSame(noEncontrado.getResponse(), new ErrorInesperadoMapper().toResponse(noEncontrado));
    }
}
