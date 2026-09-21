package tse.cargauy.negocio.vehiculo;

import java.util.List;
import jakarta.ejb.Remote;
import tse.cargauy.dtos.VehiculoDto;

@Remote
public interface VehiculoEJBRemote {
    VehiculoDto getVehiculoById(Long id);
    List<VehiculoDto> getAll();
    List<VehiculoDto> getByEmpresa(Long idEmpresa);
    void addVehiculo(VehiculoDto vehiculoDto);
    void updateVehiculo(Long id, VehiculoDto vehiculoDto);
    void deleteVehiculo(Long id);
}
