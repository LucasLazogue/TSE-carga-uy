package tse.cargauy.negocio.viaje;

import jakarta.ejb.Local;
import tse.cargauy.dtos.FiltroViajes;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.ViajeDto;

@Local
public interface ViajeEJBLocal {
    ViajeDto getViajeById(Long id);
    ViajeDto getByGuia(Long idGuia);
    PaginaDto<ViajeDto> getAll(FiltroViajes filtro, Paginacion paginacion);
    ViajeDto asignarViaje(ViajeDto viajeDto);
    void reasignarViaje(Long id, ViajeDto viajeDto);
    void deleteViaje(Long id);
}
