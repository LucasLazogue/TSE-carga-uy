package tse.cargauy.nodobalanzas.ws;

import jakarta.ejb.EJBAccessException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import tse.cargauy.nodobalanzas.exceptions.CodigoError;
import tse.cargauy.nodobalanzas.exceptions.NodoBalanzasException;

@Provider
public class AccesoDenegadoMapper implements ExceptionMapper<EJBAccessException> {

    @Context
    SecurityContext seguridad;

    @Override
    public Response toResponse(EJBAccessException exception) {
        CodigoError codigo = seguridad.getUserPrincipal() == null ? CodigoError.AUTH_CLAVE_INVALIDA : CodigoError.ACCESO_DENEGADO;
        return new NodoBalanzasExceptionMapper().toResponse(new NodoBalanzasException(codigo));
    }
}
