package tse.cargauy.web.ws;

import jakarta.ejb.EJBAccessException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@Provider
public class AccesoDenegadoMapper implements ExceptionMapper<EJBAccessException> {

    @Context
    SecurityContext seguridad;

    @Override
    public Response toResponse(EJBAccessException exception) {
        CodigoError codigo = seguridad.getUserPrincipal() == null ? CodigoError.AUTH_SESION_INVALIDA : CodigoError.ACCESO_DENEGADO;
        return new CargaUYExceptionMapper().toResponse(new CargaUYException(codigo));
    }
}
