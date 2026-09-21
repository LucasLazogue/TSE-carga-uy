package tse.cargauy.negocio.permiso;

import java.time.LocalDate;
import java.util.List;
import jakarta.ejb.Local;
import tse.cargauy.dtos.PermisoDto;

@Local
public interface PermisoEJBLocal {
    PermisoDto getPermisoById(Long id);
    List<PermisoDto> getAll();
    List<PermisoDto> getByVehiculo(Long idVehiculo);
    PermisoDto getVigente(Long idVehiculo, LocalDate fecha);
    void addPermiso(PermisoDto permisoDto);
    void updatePermiso(Long id, PermisoDto permisoDto);
    void deletePermiso(Long id);
}
