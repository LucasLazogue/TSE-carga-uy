package tse.cargauy.negocio.publico;

import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.HabilitacionEmpresaDto;
import tse.cargauy.dtos.IndicadoresPublicosDto;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;

@Local
public interface ConsultaPublicaEJBLocal {
    HabilitacionEmpresaDto validarEmpresa(Integer nroEmpresa, String razonSocial);
    IndicadoresPublicosDto getIndicadores(FiltroInforme filtro);
    List<RubroDto> getRubros();
    List<TipoCargaDto> getTiposCarga();
}
