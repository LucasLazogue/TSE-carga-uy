package tse.cargauy.negocio.informe;

import jakarta.ejb.Local;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.IndicadoresDto;
import tse.cargauy.dtos.InformeDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.entities.FormatoInforme;
import tse.cargauy.entities.TipoInforme;

@Local
public interface InformeEJBLocal {
    IndicadoresDto consultar(FiltroInforme filtro);
    PaginaDto<InformeDto> getMisInformes(Paginacion paginacion);
    InformeDto exportar(FiltroInforme filtro, TipoInforme tipo, FormatoInforme formato);
    boolean tienePendientes();
    byte[] descargar(Long id);
}
