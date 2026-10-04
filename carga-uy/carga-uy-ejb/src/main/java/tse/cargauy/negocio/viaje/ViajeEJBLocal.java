package tse.cargauy.negocio.viaje;

import java.util.List;
import jakarta.ejb.Local;
import tse.cargauy.dtos.ViajeDto;

@Local
public interface ViajeEJBLocal {
    ViajeDto getViajeById(Long id);
    ViajeDto getByGuia(Long idGuia);
    List<ViajeDto> getByEmpresa(Long idEmpresa);
    List<ViajeDto> getByChofer(Long idChofer);
    List<ViajeDto> getByVehiculo(Long idVehiculo);
    ViajeDto asignarViaje(ViajeDto viajeDto);
    void reasignarViaje(Long id, ViajeDto viajeDto);
    void deleteViaje(Long id);
}
