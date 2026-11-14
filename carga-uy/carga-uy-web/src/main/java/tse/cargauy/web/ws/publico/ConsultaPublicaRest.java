package tse.cargauy.web.ws.publico;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import tse.cargauy.dtos.HabilitacionEmpresaDto;
import tse.cargauy.negocio.publico.ConsultaPublicaEJBLocal;

@RequestScoped
@Path("/publico")
@Produces("application/json")
public class ConsultaPublicaRest {

    @EJB
    ConsultaPublicaEJBLocal consultaPublicaEJB;

    @GET
    @Path("/empresas/habilitacion")
    public HabilitacionEmpresaDto validarEmpresa(@QueryParam("nro") Integer nro,
            @QueryParam("razonSocial") String razonSocial) {
        return consultaPublicaEJB.validarEmpresa(nro, razonSocial);
    }
}
