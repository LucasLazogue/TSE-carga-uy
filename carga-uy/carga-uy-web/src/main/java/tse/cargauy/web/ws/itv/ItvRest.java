package tse.cargauy.web.ws.itv;

import java.util.List;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import tse.cargauy.dtos.ItvDto;
import tse.cargauy.negocio.itv.ItvEJBLocal;

@RequestScoped
@Path("/vehiculos/{vehiculo}/itv")
@Consumes("application/json")
@Produces("application/json")
public class ItvRest {

    @EJB
    ItvEJBLocal itvEJB;

    public ItvRest() {
    }

    @GET
    public List<ItvDto> getItvs(@PathParam("vehiculo") Long idVehiculo) {
        return itvEJB.listar(idVehiculo);
    }

    @POST
    public void createItv(@PathParam("vehiculo") Long idVehiculo, ItvDto itv) {
        itvEJB.agregar(idVehiculo, itv);
    }

    @Path("/{id}")
    @DELETE
    public void deleteItv(@PathParam("vehiculo") Long idVehiculo, @PathParam("id") Long id) {
        itvEJB.eliminar(idVehiculo, id);
    }
}
