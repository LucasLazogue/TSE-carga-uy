package tse.cargauy.negocio.viaje;

import jakarta.ejb.Remote;
import tse.cargauy.dtos.FiltroViajes;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.ViajeDto;

@Remote
public interface ViajeEJBRemote {

    PaginaDto<ViajeDto> listar(Long idEmpresa, FiltroViajes filtro, Paginacion paginacion);

    ViajeDto getViaje(Long idEmpresa, Long id);

    ViajeDto asignarViaje(Long idEmpresa, ViajeDto viajeDto);

    ViajeDto reasignarViaje(Long idEmpresa, Long id, ViajeDto viajeDto);

    void deleteViaje(Long idEmpresa, Long id);
}
