package tse.cargauy.web.ws.casos;

import java.util.List;

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
import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.PistaAuditoriaDto;
import tse.cargauy.dtos.ResolucionDto;
import tse.cargauy.negocio.fiscalizacion.CasoFiscalizacionEJBLocal;
import tse.cargauy.web.ws.PaginacionQuery;

@RequestScoped
@Path("/casos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CasosRest {

    @EJB
    CasoFiscalizacionEJBLocal casoEJB;

    @GET
    public PaginaDto<CasoFiscalizacionDto> getCasos(@BeanParam CasosQuery query, @BeanParam PaginacionQuery paginacion) {
        return casoEJB.listar(query.filtro(), paginacion.paginacion());
    }

    @GET
    @Path("/{id}")
    public CasoFiscalizacionDto getCaso(@PathParam("id") Long id) {
        return casoEJB.getCaso(id);
    }

    @GET
    @Path("/{id}/auditoria")
    public List<PistaAuditoriaDto> getAuditoria(@PathParam("id") Long id) {
        return casoEJB.getAuditoria(id);
    }

    @POST
    @Path("/{id}/resolucion")
    public Response resolver(@PathParam("id") Long id, ResolucionDto resolucion) {
        return Response.status(Response.Status.CREATED).entity(casoEJB.resolver(id, resolucion)).build();
    }
}
