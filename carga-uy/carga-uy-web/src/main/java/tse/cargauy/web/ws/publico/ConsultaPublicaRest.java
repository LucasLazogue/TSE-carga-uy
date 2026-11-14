package tse.cargauy.web.ws.publico;

import java.time.LocalDate;
import java.util.List;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.HabilitacionEmpresaDto;
import tse.cargauy.dtos.IndicadoresPublicosDto;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;
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

    @GET
    @Path("/indicadores")
    public IndicadoresPublicosDto getIndicadores(@QueryParam("desde") LocalDate desde, @QueryParam("hasta") LocalDate hasta,
            @QueryParam("tipoCarga") Long idTipoCarga, @QueryParam("rubro") Long idRubro) {
        return consultaPublicaEJB.getIndicadores(new FiltroInforme(desde, hasta, idTipoCarga, idRubro));
    }

    @GET
    @Path("/rubros")
    public List<RubroDto> getRubros() {
        return consultaPublicaEJB.getRubros();
    }

    @GET
    @Path("/tipos-carga")
    public List<TipoCargaDto> getTiposCarga() {
        return consultaPublicaEJB.getTiposCarga();
    }
}
