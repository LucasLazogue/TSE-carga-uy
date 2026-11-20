package tse.cargauy.nodobalanzas.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.security.Principal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import jakarta.ejb.EJBAccessException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.ParamConverter;
import tse.cargauy.nodobalanzas.exceptions.CodigoError;
import tse.cargauy.nodobalanzas.exceptions.NodoBalanzasException;
import tse.cargauy.nodobalanzas.ws.converters.FechaParamConverterProvider;

// los errores de la API del nodo salen con su codigo HTTP y un cuerpo sin detalles internos
class ErroresTest {

    @Test
    void unErrorDelNodoSaleConSuCategoria() {
        Response respuesta = new NodoBalanzasExceptionMapper().toResponse(new NodoBalanzasException(CodigoError.PEAJE_NO_ENCONTRADO));

        assertEquals(404, respuesta.getStatus());
        assertEquals(CodigoError.PEAJE_NO_ENCONTRADO.getCodigo(), ((ErrorDto) respuesta.getEntity()).getCodigo());
    }

    // sin clave es 401; con una clave valida pero de otro rol (por ejemplo una balanza consultando) es 403
    @Test
    void unAccesoDenegadoEs401SinClaveY403ConOtroRol() {
        AccesoDenegadoMapper mapper = new AccesoDenegadoMapper();
        mapper.seguridad = contexto(null);
        Response sinClave = mapper.toResponse(new EJBAccessException());
        mapper.seguridad = contexto(() -> "balanza-1");
        Response otroRol = mapper.toResponse(new EJBAccessException());

        assertEquals(401, sinClave.getStatus());
        assertEquals(CodigoError.AUTH_CLAVE_INVALIDA.getCodigo(), ((ErrorDto) sinClave.getEntity()).getCodigo());
        assertEquals(403, otroRol.getStatus());
    }

    private static SecurityContext contexto(Principal usuario) {
        return (SecurityContext) Proxy.newProxyInstance(ErroresTest.class.getClassLoader(), new Class<?>[] { SecurityContext.class },
                (proxy, metodo, args) -> metodo.getName().equals("getUserPrincipal") ? usuario : null);
    }

    @Test
    void unErrorInesperadoNoMuestraElDetalle() {
        Response respuesta = new ErrorInesperadoMapper().toResponse(new IllegalStateException("detalle interno"));
        NotFoundException noEncontrado = new NotFoundException();

        assertEquals(500, respuesta.getStatus());
        assertEquals("Ocurrio un error inesperado.", ((ErrorDto) respuesta.getEntity()).getMensaje());
        assertSame(noEncontrado.getResponse(), new ErrorInesperadoMapper().toResponse(noEncontrado));
    }

    @Test
    void lasFechasDeLaConsulta() {
        ParamConverter<LocalDate> fecha = new FechaParamConverterProvider().getConverter(LocalDate.class, LocalDate.class, null);

        assertEquals(LocalDate.of(2026, 10, 20), fecha.fromString("2026-10-20"));
        assertNull(fecha.fromString(" "));
        assertNull(fecha.fromString(null));
        assertEquals("2026-10-20", fecha.toString(LocalDate.of(2026, 10, 20)));
        assertNull(new FechaParamConverterProvider().getConverter(String.class, String.class, null));
    }

    @Test
    void elErrorTieneCodigoYMensaje() {
        ErrorDto error = new ErrorDto(1001, "falta la fecha");
        error.setCodigo(1002);
        error.setMensaje("falta la matricula");

        assertEquals(1002, error.getCodigo());
        assertEquals("falta la matricula", error.getMensaje());
    }
}
