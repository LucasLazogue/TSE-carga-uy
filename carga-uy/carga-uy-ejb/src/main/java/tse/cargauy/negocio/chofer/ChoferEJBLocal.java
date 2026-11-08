package tse.cargauy.negocio.chofer;

import jakarta.ejb.Local;
import tse.cargauy.dtos.ChoferDto;
import tse.cargauy.dtos.FiltroChoferes;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;

@Local
public interface ChoferEJBLocal {
    PaginaDto<ChoferDto> getChoferes(Long idEmpresa, FiltroChoferes filtro, Paginacion paginacion);
    ChoferDto addChofer(Long idEmpresa, ChoferDto choferDto);
    ChoferDto darDeBaja(Long idEmpresa, Long idVinculo);
}
