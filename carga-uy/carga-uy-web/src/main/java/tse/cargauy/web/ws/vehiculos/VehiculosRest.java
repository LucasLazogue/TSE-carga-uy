package tse.cargauy.web.ws.vehiculos;

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
import jakarta.ws.rs.QueryParam;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.negocio.vehiculo.VehiculoEJBLocal;
import tse.cargauy.web.ws.PaginacionQuery;

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
    public PaginaDto<VehiculoDto> getVehiculos(@QueryParam("idEmpresa") Long idEmpresa, @BeanParam PaginacionQuery paginacion) {
        if (idEmpresa == null) {
            return vehiculoEJB.getAll(paginacion.paginacion());
        }
        return vehiculoEJB.getByEmpresa(idEmpresa, paginacion.paginacion());
    }

    @Path("/{id}")
    @GET
    public VehiculoDto getVehiculoById(@PathParam("id") Long id) {
        return vehiculoEJB.getVehiculoById(id);
    }
}
