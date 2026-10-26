package tse.cargauy.adaptadores.balanzas;

import java.time.LocalDate;
import java.util.List;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/api/pesajes")
@Produces(MediaType.APPLICATION_JSON)
@RegisterProvider(RechazoNodoMapper.class)
public interface NodoBalanzasApi {

    @GET
    List<PesajeNodo> getPesajes(@HeaderParam("Authorization") String autorizacion,
            @QueryParam("matricula") String matricula, @QueryParam("fecha") LocalDate fecha);
}
