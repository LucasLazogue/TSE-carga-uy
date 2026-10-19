package tse.cargauy.web.ws.infracciones;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.dtos.DescargoDto;
import tse.cargauy.dtos.NotificacionDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.negocio.fiscalizacion.CasoFiscalizacionEJBLocal;
import tse.cargauy.web.ws.PaginacionQuery;

@RequestScoped
@Path("/infracciones")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InfraccionesRest {

    @EJB
    CasoFiscalizacionEJBLocal casoEJB;

    @GET
    public PaginaDto<CasoFiscalizacionDto> getInfracciones(@QueryParam("empresa") Long idEmpresa, @QueryParam("estado") EstadoCaso estado,
            @BeanParam PaginacionQuery paginacion) {
        return casoEJB.listarInfracciones(idEmpresa, estado, paginacion.paginacion());
    }

    @GET
    @Path("/notificaciones")
    public PaginaDto<NotificacionDto> getNotificaciones(@BeanParam PaginacionQuery paginacion) {
        return casoEJB.getNotificaciones(paginacion.paginacion());
    }

    @GET
    @Path("/{id}")
    public CasoFiscalizacionDto getInfraccion(@PathParam("id") Long id) {
        return casoEJB.getInfraccion(id);
    }

    @POST
    @Path("/{id}/descargo")
    public Response presentarDescargo(@PathParam("id") Long id, DescargoDto descargo) {
        return Response.status(Response.Status.CREATED).entity(casoEJB.presentarDescargo(id, descargo)).build();
    }
}
