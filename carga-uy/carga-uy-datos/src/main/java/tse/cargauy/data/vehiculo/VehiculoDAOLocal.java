package tse.cargauy.data.vehiculo;

import jakarta.ejb.Local;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.VehiculoDto;

@Local
public interface VehiculoDAOLocal {
    VehiculoDto getVehiculoById(Long id);
    VehiculoDto getVehiculoByMatricula(String matricula);
    PaginaDto<VehiculoDto> getAll(Paginacion paginacion);
    PaginaDto<VehiculoDto> getByEmpresa(Long idEmpresa, Paginacion paginacion);
    void addVehiculo(VehiculoDto vehiculoDto);
    void updateVehiculo(Long id, VehiculoDto vehiculoDto);
    void deleteVehiculo(Long id);
}
