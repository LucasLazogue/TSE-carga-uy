package tse.cargauy.adaptadores.balanzas;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

// un 4xx del nodo (clave mala, consulta invalida) no se arregla reintentando: se convierte en CargaUYException
// para que no cuente como falla del nodo. los 5xx siguen como error comun y si se reintentan
public class RechazoNodoMapper implements ResponseExceptionMapper<CargaUYException> {

    @Override
    public boolean handles(int status, MultivaluedMap<String, Object> headers) {
        return status >= 400 && status < 500;
    }

    @Override
    public CargaUYException toThrowable(Response response) {
        return new CargaUYException(CodigoError.BALANZAS_NODO_RECHAZO, String.valueOf(response.getStatus()));
    }
}
