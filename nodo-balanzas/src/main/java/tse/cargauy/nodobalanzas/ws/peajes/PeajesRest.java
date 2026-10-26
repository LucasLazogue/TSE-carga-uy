package tse.cargauy.nodobalanzas.ws.peajes;

import java.util.List;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import tse.cargauy.nodobalanzas.dtos.BalanzaDto;
import tse.cargauy.nodobalanzas.dtos.PeajeDto;
import tse.cargauy.nodobalanzas.negocio.peaje.PeajeEJBLocal;

@RequestScoped
@Path("/peajes")
@Consumes("application/json")
@Produces("application/json")
public class PeajesRest {

    @EJB
    PeajeEJBLocal peajeEJB;

    public PeajesRest() {
    }

    @GET
    public List<PeajeDto> getPeajes() {
        return peajeEJB.getPeajes();
    }

    @POST
    public void createPeaje(PeajeDto peajeDto) {
        peajeEJB.addPeaje(peajeDto);
    }

    @Path("/{codigo}/balanzas")
    @POST
    public BalanzaDto createBalanza(@PathParam("codigo") String codigoPeaje, BalanzaDto balanzaDto) {
        return peajeEJB.addBalanza(codigoPeaje, balanzaDto);
    }
}
