package tse.cargauy.web.ws.movil;

import java.util.List;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.dtos.SyncRespuestaDto;
import tse.cargauy.dtos.ViajeMovilDto;
import tse.cargauy.negocio.movil.ViajeChoferEJBLocal;

@RequestScoped
@Path("/movil")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MovilRest {

    @EJB
    ViajeChoferEJBLocal viajeChoferEJB;

    @GET
    @Path("/viaje")
    public Response getViaje() {
        ViajeMovilDto viaje = viajeChoferEJB.getViajeActual();
        return viaje == null ? Response.noContent().build() : Response.ok(viaje).build();
    }

    @POST
    @Path("/sync")
    public SyncRespuestaDto sincronizar(List<EventoViajeDto> eventos) {
        return viajeChoferEJB.sincronizar(eventos);
    }
}
