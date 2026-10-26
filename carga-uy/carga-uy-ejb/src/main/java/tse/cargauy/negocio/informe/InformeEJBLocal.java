package tse.cargauy.negocio.informe;

import jakarta.ejb.Local;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.IndicadoresDto;

@Local
public interface InformeEJBLocal {
    IndicadoresDto consultar(FiltroInforme filtro);
}
