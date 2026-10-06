package tse.cargauy.web.ws.posiciones;

import java.util.List;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import tse.cargauy.dtos.EventoPosicionDto;
import tse.cargauy.negocio.tracking.IngestaTrackingEJBLocal;

@RequestScoped
@Path("/posiciones")
@Produces("application/json")
public class PosicionesRest {

    @EJB
    IngestaTrackingEJBLocal ingesta;

    public PosicionesRest() {
    }

    // ordenadas por el momento en que se generaron, no por el orden de llegada
    @GET
    public List<EventoPosicionDto> getRuta(@QueryParam("matricula") String matricula) {
        return ingesta.getRuta(matricula);
    }
}
