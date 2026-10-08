package tse.cargauy.web.ws.empresas;

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
import jakarta.ws.rs.core.Response;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.negocio.empresa.EmpresaEJBLocal;
import tse.cargauy.web.ws.PaginacionQuery;

@RequestScoped
@Path("/empresas")
@Consumes("application/json")
@Produces("application/json")
public class EmpresasRest {

    @EJB
    EmpresaEJBLocal empresaEJB;

    public EmpresasRest() {
    }

    @POST
    public Response createEmpresa(EmpresaDto empresaDto) {
        EmpresaDto empresa = empresaEJB.addEmpresa(empresaDto);
        return Response.status(Response.Status.CREATED).entity(empresa).build();
    }

    @Path("/{id}")
    @PUT
    public void updateEmpresa(@PathParam("id") Long id, EmpresaDto empresaDto) {
        empresaEJB.updateEmpresa(id, empresaDto);
    }

    @Path("/{id}")
    @DELETE
    public void deleteEmpresa(@PathParam("id") Long id) {
        empresaEJB.deleteEmpresa(id);
    }

    @GET
    public PaginaDto<EmpresaDto> getEmpresas(@QueryParam("nombre") String nombre, @BeanParam PaginacionQuery paginacion) {
        if (nombre == null || nombre.isEmpty()) {
            return empresaEJB.getAll(paginacion.paginacion());
        }
        return empresaEJB.findByNombre(nombre, paginacion.paginacion());
    }

    @Path("/{id}")
    @GET
    public EmpresaDto getEmpresaById(@PathParam("id") Long id) {
        return empresaEJB.getEmpresaById(id);
    }
}
