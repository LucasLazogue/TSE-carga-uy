package tse.cargauy.web.ws.viajes;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.negocio.viaje.ViajeEJBLocal;

@RequestScoped
@Path("/viajes")
@Consumes("application/json")
@Produces("application/json")
public class ViajesRest {

    @EJB
    ViajeEJBLocal viajeEJB;

    public ViajesRest() {
    }

    @POST
    public ViajeDto asignarViaje(ViajeDto viajeDto) {
        return viajeEJB.asignarViaje(viajeDto);
    }

    @Path("/{id}")
    @PUT
    public void reasignarViaje(@PathParam("id") Long id, ViajeDto viajeDto) {
        viajeEJB.reasignarViaje(id, viajeDto);
    }

    @Path("/{id}")
    @DELETE
    public void deleteViaje(@PathParam("id") Long id) {
        viajeEJB.deleteViaje(id);
    }

    @GET
    public PaginaDto<ViajeDto> getViajes(@BeanParam ViajesQuery query) {
        return viajeEJB.getAll(query.filtro(), query.paginacion());
    }

    @Path("/{id}")
    @GET
    public ViajeDto getViajeById(@PathParam("id") Long id) {
        return viajeEJB.getViajeById(id);
    }
}
