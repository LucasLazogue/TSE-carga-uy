package tse.cargauy.negocio.chofer;

import jakarta.ejb.Remote;
import tse.cargauy.dtos.ChoferDto;
import tse.cargauy.dtos.FiltroChoferes;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;

@Remote
public interface ChoferEJBRemote {
    PaginaDto<ChoferDto> getChoferes(Long idEmpresa, FiltroChoferes filtro, Paginacion paginacion);
    ChoferDto addChofer(Long idEmpresa, ChoferDto choferDto);
    ChoferDto darDeBaja(Long idEmpresa, Long idVinculo);
}
