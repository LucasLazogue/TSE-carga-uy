package tse.cargauy.negocio.publico;

import java.time.LocalDate;
import java.util.List;

import jakarta.annotation.security.PermitAll;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.data.guia.GuiaDAOLocal;
import tse.cargauy.data.informe.InformeDAOLocal;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.HabilitacionEmpresaDto;
import tse.cargauy.dtos.IndicadoresPublicosDto;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.historico.HistoricoDAOLocal;
import tse.cargauy.negocio.informe.InformeEJB;

@Stateless
@PermitAll
public class ConsultaPublicaEJB implements ConsultaPublicaEJBLocal {

    static final int UMBRAL_AGREGACION = 5;

    @EJB
    EmpresaDAOLocal empresaDAO;

    @EJB
    InformeDAOLocal informeDAO;

    @EJB
    HistoricoDAOLocal historicoDAO;

    @EJB
    GuiaDAOLocal guiaDAO;

    @Override
    public IndicadoresPublicosDto getIndicadores(FiltroInforme filtro) {
        InformeEJB.validar(filtro);
        LocalDate hoy = hoy();
        IndicadoresPublicosDto indicadores = new IndicadoresPublicosDto();
        indicadores.setUmbral(UMBRAL_AGREGACION);
        indicadores.setHabilitadosAl(hoy);
        long empresas = informeDAO.contarEmpresasHabilitadas(hoy);
        long vehiculos = informeDAO.contarVehiculosHabilitados(hoy);
        if (empresas >= UMBRAL_AGREGACION && vehiculos >= UMBRAL_AGREGACION) {
            indicadores.setEmpresasHabilitadas(empresas);
            indicadores.setVehiculosHabilitados(vehiculos);
        }
        indicadores.setViajesPorMes(historicoDAO.contarViajesPorMesPublico(filtro, UMBRAL_AGREGACION));
        indicadores.setVolumenPorRubro(historicoDAO.sumarVolumenPorRubroPublico(filtro, UMBRAL_AGREGACION));
        return indicadores;
    }

    @Override
    public List<RubroDto> getRubros() {
        return guiaDAO.getRubros();
    }

    @Override
    public List<TipoCargaDto> getTiposCarga() {
        return guiaDAO.getTiposCarga();
    }

    @Override
    public HabilitacionEmpresaDto validarEmpresa(Integer nroEmpresa, String razonSocial) {
        boolean porRazonSocial = razonSocial != null && !razonSocial.isBlank();
        if ((nroEmpresa == null) == !porRazonSocial) {
            throw new CargaUYException(CodigoError.EMPRESA_CONSULTA_DATO_REQUERIDO);
        }
        EmpresaDto empresa = porRazonSocial ? porRazonSocial(razonSocial) : empresaDAO.getEmpresaByNro(nroEmpresa);
        if (empresa == null) {
            throw new CargaUYException(CodigoError.EMPRESA_CONSULTA_SIN_RESULTADO);
        }
        LocalDate hoy = hoy();
        return new HabilitacionEmpresaDto(empresa.getNombrePublico(), empresa.getRazonSocial(),
                informeDAO.isEmpresaHabilitada(empresa.getId(), hoy), hoy);
    }

    private EmpresaDto porRazonSocial(String razonSocial) {
        List<EmpresaDto> empresas = empresaDAO.getByRazonSocial(razonSocial, 2);
        if (empresas.size() > 1) {
            throw new CargaUYException(CodigoError.EMPRESA_CONSULTA_AMBIGUA);
        }
        return empresas.isEmpty() ? null : empresas.get(0);
    }

    LocalDate hoy() {
        return LocalDate.now();
    }
}
