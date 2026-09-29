package tse.cargauy.web.ws;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.exceptions.MensajesError;

@Provider
public class CargaUYExceptionMapper implements ExceptionMapper<CargaUYException> {

    @Override
    public Response toResponse(CargaUYException exception) {
        CodigoError codigo = exception.getCodigo();

        return Response.status(codigo.getStatus())
                .entity(new ErrorDto(codigo.getCodigo(), MensajesError.resolver(exception)))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
