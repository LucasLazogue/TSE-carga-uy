package tse.cargauy.web.ws.choferes;

import java.time.LocalDate;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.negocio.empresa.EmpresaEJBLocal;
import tse.cargauy.web.ws.PaginacionQuery;

@RequestScoped
@Path("/empresas/{empresa}/choferes")
@Produces(MediaType.APPLICATION_JSON)
public class ChoferesRest {

    @EJB
    EmpresaEJBLocal empresaEJB;

    @GET
    public PaginaDto<UsuarioDto> getChoferes(@PathParam("empresa") Long idEmpresa, @QueryParam("cedula") String cedula,
            @QueryParam("fecha") LocalDate fecha, @BeanParam PaginacionQuery paginacion) {
        return empresaEJB.getChoferes(idEmpresa, cedula, fecha, paginacion.paginacion());
    }
}
