package tse.cargauy.web.ws.guias;

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
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.negocio.guia.GuiaEJBLocal;
import tse.cargauy.web.ws.PaginacionQuery;

@RequestScoped
@Path("/empresas/{empresa}/guias")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GuiasRest {

    @EJB
    GuiaEJBLocal guiaEJB;

    @GET
    public PaginaDto<GuiaDto> getGuias(@PathParam("empresa") Long idEmpresa, @BeanParam GuiasQuery query, @BeanParam PaginacionQuery paginacion) {
        return guiaEJB.listar(idEmpresa, query.filtro(), paginacion.paginacion());
    }

    @POST
    public Response createGuia(@PathParam("empresa") Long idEmpresa, GuiaDto guiaDto) {
        return Response.status(Response.Status.CREATED).entity(guiaEJB.addGuia(idEmpresa, guiaDto)).build();
    }

    @GET
    @Path("/{id}")
    public GuiaDto getGuia(@PathParam("empresa") Long idEmpresa, @PathParam("id") Long id) {
        return guiaEJB.getGuia(idEmpresa, id);
    }

    @PUT
    @Path("/{id}")
    public GuiaDto updateGuia(@PathParam("empresa") Long idEmpresa, @PathParam("id") Long id, GuiaDto guiaDto) {
        return guiaEJB.updateGuia(idEmpresa, id, guiaDto);
    }

    @DELETE
    @Path("/{id}")
    public void deleteGuia(@PathParam("empresa") Long idEmpresa, @PathParam("id") Long id) {
        guiaEJB.deleteGuia(idEmpresa, id);
    }
}
