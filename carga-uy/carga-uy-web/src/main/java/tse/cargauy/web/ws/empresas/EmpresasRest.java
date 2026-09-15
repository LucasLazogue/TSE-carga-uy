package tse.cargauy.web.ws.empresas;

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
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.negocio.empresa.EmpresaEJBLocal;

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
    public void createEmpresa(EmpresaDto empresaDto) {
        empresaEJB.addEmpresa(empresaDto);
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
    public List<EmpresaDto> getEmpresas(@QueryParam("nombre") String nombre) {
        if (nombre == null || nombre.isEmpty()) {
            return empresaEJB.getAll();
        }
        return empresaEJB.findByNombre(nombre);
    }

    @Path("/{id}")
    @GET
    public EmpresaDto getEmpresaById(@PathParam("id") Long id) {
        return empresaEJB.getEmpresaById(id);
    }
}
