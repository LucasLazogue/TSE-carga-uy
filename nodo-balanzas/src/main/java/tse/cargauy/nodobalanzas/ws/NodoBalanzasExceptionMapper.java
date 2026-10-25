package tse.cargauy.nodobalanzas.ws;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import tse.cargauy.nodobalanzas.exceptions.CodigoError;
import tse.cargauy.nodobalanzas.exceptions.MensajesError;
import tse.cargauy.nodobalanzas.exceptions.NodoBalanzasException;

@Provider
public class NodoBalanzasExceptionMapper implements ExceptionMapper<NodoBalanzasException> {

    @Override
    public Response toResponse(NodoBalanzasException exception) {
        CodigoError codigo = exception.getCodigo();

        return Response.status(codigo.getCategoria().getStatus())
                .entity(new ErrorDto(codigo.getCodigo(), MensajesError.resolver(exception)))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
