package tse.cargauy.web.ws.guias;

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
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.negocio.guia.GuiaEJBLocal;

@RequestScoped
@Path("/guias")
@Consumes("application/json")
@Produces("application/json")
public class GuiasRest {

    @EJB
    GuiaEJBLocal guiaEJB;

    public GuiasRest() {
    }

    @POST
    public GuiaDto createGuia(GuiaDto guiaDto) {
        return guiaEJB.addGuia(guiaDto);
    }

    @Path("/{id}")
    @PUT
    public void updateGuia(@PathParam("id") Long id, GuiaDto guiaDto) {
        guiaEJB.updateGuia(id, guiaDto);
    }

    @Path("/{id}")
    @DELETE
    public void deleteGuia(@PathParam("id") Long id) {
        guiaEJB.deleteGuia(id);
    }

    @GET
    public List<GuiaDto> getGuias(@QueryParam("idEmpresa") Long idEmpresa) {
        if (idEmpresa == null) {
            return guiaEJB.getAll();
        }
        return guiaEJB.getByEmpresa(idEmpresa);
    }

    @Path("/{id}")
    @GET
    public GuiaDto getGuiaById(@PathParam("id") Long id) {
        return guiaEJB.getGuiaById(id);
    }

    @Path("/rubros")
    @GET
    public List<RubroDto> getRubros() {
        return guiaEJB.getRubros();
    }

    @Path("/tipos-carga")
    @GET
    public List<TipoCargaDto> getTiposCarga() {
        return guiaEJB.getTiposCarga();
    }
}
