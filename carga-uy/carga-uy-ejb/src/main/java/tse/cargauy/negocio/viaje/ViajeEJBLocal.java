package tse.cargauy.negocio.viaje;

import jakarta.ejb.Local;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.dtos.FiltroViajes;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PesajesViajeDto;
import tse.cargauy.dtos.ViajeDto;

@Local
public interface ViajeEJBLocal {

    PaginaDto<ViajeDto> listar(Long idEmpresa, FiltroViajes filtro, Paginacion paginacion);

    ViajeDto getViaje(Long idEmpresa, Long id);

    PaginaDto<EventoViajeDto> getEventos(Long idEmpresa, Long id, Paginacion paginacion);
    PesajesViajeDto getPesajes(Long idEmpresa, Long id);

    ViajeDto asignarViaje(Long idEmpresa, ViajeDto viajeDto);

    ViajeDto reasignarViaje(Long idEmpresa, Long id, ViajeDto viajeDto);

    void deleteViaje(Long idEmpresa, Long id);
}
