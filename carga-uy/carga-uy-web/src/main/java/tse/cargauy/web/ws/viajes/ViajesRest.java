package tse.cargauy.web.ws.viajes;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.negocio.viaje.ViajeEJBLocal;

@RequestScoped
@Path("/empresas/{empresa}/viajes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ViajesRest {

    @EJB
    ViajeEJBLocal viajeEJB;

    @GET
    public PaginaDto<ViajeDto> getViajes(@PathParam("empresa") Long idEmpresa, @BeanParam ViajesQuery query) {
        return viajeEJB.listar(idEmpresa, query.filtro(), query.paginacion());
    }

    @POST
    public Response asignarViaje(@PathParam("empresa") Long idEmpresa, ViajeDto viajeDto) {
        return Response.status(Response.Status.CREATED).entity(viajeEJB.asignarViaje(idEmpresa, viajeDto)).build();
    }

    @GET
    @Path("/{id}")
    public ViajeDto getViaje(@PathParam("empresa") Long idEmpresa, @PathParam("id") Long id) {
        return viajeEJB.getViaje(idEmpresa, id);
    }

    @PUT
    @Path("/{id}")
    public ViajeDto reasignarViaje(@PathParam("empresa") Long idEmpresa, @PathParam("id") Long id, ViajeDto viajeDto) {
        return viajeEJB.reasignarViaje(idEmpresa, id, viajeDto);
    }

    @DELETE
    @Path("/{id}")
    public void deleteViaje(@PathParam("empresa") Long idEmpresa, @PathParam("id") Long id) {
        viajeEJB.deleteViaje(idEmpresa, id);
    }
}
