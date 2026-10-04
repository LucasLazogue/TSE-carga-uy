package tse.cargauy.data.viaje;

import java.util.List;
import jakarta.ejb.Local;
import tse.cargauy.dtos.ViajeDto;

@Local
public interface ViajeDAOLocal {
    ViajeDto getViajeById(Long id);
    ViajeDto getByGuia(Long idGuia);
    List<ViajeDto> getByEmpresa(Long idEmpresa);
    List<ViajeDto> getByChofer(Long idChofer);
    List<ViajeDto> getByVehiculo(Long idVehiculo);
    ViajeDto addViaje(ViajeDto viajeDto);
    void updateViaje(Long id, ViajeDto viajeDto);
    void deleteViaje(Long id);
}
