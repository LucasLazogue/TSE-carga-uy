package tse.cargauy.web.ws.vehiculos;

import java.util.List;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.negocio.vehiculo.VehiculoEJBLocal;

@RequestScoped
@Path("/vehiculos")
@Consumes("application/json")
@Produces("application/json")
public class VehiculosRest {

    @EJB
    VehiculoEJBLocal vehiculoEJB;

    public VehiculosRest() {
    }

    @POST
    public void createVehiculo(VehiculoDto vehiculoDto) {
        vehiculoEJB.addVehiculo(vehiculoDto);
    }

    @Path("/{id}")
    @PUT
    public void updateVehiculo(@PathParam("id") Long id, VehiculoDto vehiculoDto) {
        vehiculoEJB.updateVehiculo(id, vehiculoDto);
    }

    @Path("/{id}")
    @DELETE
    public void deleteVehiculo(@PathParam("id") Long id) {
        vehiculoEJB.deleteVehiculo(id);
    }

    @GET
    public List<VehiculoDto> getVehiculos(@QueryParam("idEmpresa") Long idEmpresa) {
        if (idEmpresa == null) {
            return vehiculoEJB.getAll();
        }
        return vehiculoEJB.getByEmpresa(idEmpresa);
    }

    @Path("/{id}")
    @GET
    public VehiculoDto getVehiculoById(@PathParam("id") Long id) {
        return vehiculoEJB.getVehiculoById(id);
    }
}
