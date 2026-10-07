package tse.cargauy.web.ws.catalogos;

import java.util.List;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.negocio.guia.GuiaEJBLocal;

@RequestScoped
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class CatalogosRest {

    @EJB
    GuiaEJBLocal guiaEJB;

    @GET
    @Path("/rubros")
    public List<RubroDto> getRubros() {
        return guiaEJB.getRubros();
    }

    @GET
    @Path("/tipos-carga")
    public List<TipoCargaDto> getTiposCarga() {
        return guiaEJB.getTiposCarga();
    }
}
