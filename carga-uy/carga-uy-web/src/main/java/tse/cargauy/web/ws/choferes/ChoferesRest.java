package tse.cargauy.web.ws.choferes;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tse.cargauy.dtos.ChoferDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.negocio.chofer.ChoferEJBLocal;
import tse.cargauy.web.ws.PaginacionQuery;

@RequestScoped
@Path("/empresas/{empresa}/choferes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ChoferesRest {

    @EJB
    ChoferEJBLocal choferEJB;

    @GET
    public PaginaDto<ChoferDto> getChoferes(@PathParam("empresa") Long idEmpresa, @BeanParam ChoferesQuery query, @BeanParam PaginacionQuery paginacion) {
        return choferEJB.getChoferes(idEmpresa, query.filtro(), paginacion.paginacion());
    }

    @POST
    public Response addChofer(@PathParam("empresa") Long idEmpresa, ChoferDto choferDto) {
        return Response.status(Response.Status.CREATED).entity(choferEJB.addChofer(idEmpresa, choferDto)).build();
    }

    @POST
    @Path("/{idVinculo}/baja")
    public ChoferDto darDeBaja(@PathParam("empresa") Long idEmpresa, @PathParam("idVinculo") Long idVinculo) {
        return choferEJB.darDeBaja(idEmpresa, idVinculo);
    }
}
