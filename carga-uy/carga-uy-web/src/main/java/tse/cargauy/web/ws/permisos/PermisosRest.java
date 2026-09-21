package tse.cargauy.web.ws.permisos;

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
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.negocio.permiso.PermisoEJBLocal;

@RequestScoped
@Path("/permisos")
@Consumes("application/json")
@Produces("application/json")
public class PermisosRest {

    @EJB
    PermisoEJBLocal permisoEJB;

    public PermisosRest() {
    }

    @POST
    public void createPermiso(PermisoDto permisoDto) {
        permisoEJB.addPermiso(permisoDto);
    }

    @Path("/{id}")
    @PUT
    public void updatePermiso(@PathParam("id") Long id, PermisoDto permisoDto) {
        permisoEJB.updatePermiso(id, permisoDto);
    }

    @Path("/{id}")
    @DELETE
    public void deletePermiso(@PathParam("id") Long id) {
        permisoEJB.deletePermiso(id);
    }

    @GET
    public List<PermisoDto> getPermisos(@QueryParam("idVehiculo") Long idVehiculo) {
        if (idVehiculo == null) {
            return permisoEJB.getAll();
        }
        return permisoEJB.getByVehiculo(idVehiculo);
    }

    @Path("/{id}")
    @GET
    public PermisoDto getPermisoById(@PathParam("id") Long id) {
        return permisoEJB.getPermisoById(id);
    }
}
