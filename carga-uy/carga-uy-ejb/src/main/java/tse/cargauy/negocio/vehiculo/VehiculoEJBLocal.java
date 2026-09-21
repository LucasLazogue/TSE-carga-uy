package tse.cargauy.negocio.vehiculo;

import java.util.List;
import jakarta.ejb.Local;
import tse.cargauy.dtos.VehiculoDto;

@Local
public interface VehiculoEJBLocal {
    VehiculoDto getVehiculoById(Long id);
    List<VehiculoDto> getAll();
    List<VehiculoDto> getByEmpresa(Long idEmpresa);
    void addVehiculo(VehiculoDto vehiculoDto);
    void updateVehiculo(Long id, VehiculoDto vehiculoDto);
    void deleteVehiculo(Long id);
}
