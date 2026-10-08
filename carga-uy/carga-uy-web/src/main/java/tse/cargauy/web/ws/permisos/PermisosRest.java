package tse.cargauy.web.ws.permisos;

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
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.negocio.permiso.PermisoEJBLocal;
import tse.cargauy.web.ws.PaginacionQuery;

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
    public PaginaDto<PermisoDto> getPermisos(@QueryParam("idVehiculo") Long idVehiculo, @BeanParam PaginacionQuery paginacion) {
        if (idVehiculo == null) {
            return permisoEJB.getAll(paginacion.paginacion());
        }
        return permisoEJB.getByVehiculo(idVehiculo, paginacion.paginacion());
    }

    @Path("/{id}")
    @GET
    public PermisoDto getPermisoById(@PathParam("id") Long id) {
        return permisoEJB.getPermisoById(id);
    }
}
