package tse.cargauy.negocio.vehiculo;

import jakarta.ejb.Remote;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.VehiculoDto;

@Remote
public interface VehiculoEJBRemote {
    VehiculoDto getVehiculoById(Long id);
    PaginaDto<VehiculoDto> getAll(Paginacion paginacion);
    PaginaDto<VehiculoDto> getByEmpresa(Long idEmpresa, Paginacion paginacion);
    void addVehiculo(VehiculoDto vehiculoDto);
    void updateVehiculo(Long id, VehiculoDto vehiculoDto);
    void deleteVehiculo(Long id);
}
