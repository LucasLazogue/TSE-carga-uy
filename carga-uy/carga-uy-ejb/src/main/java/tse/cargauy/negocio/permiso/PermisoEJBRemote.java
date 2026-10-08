package tse.cargauy.negocio.permiso;

import java.time.LocalDate;
import jakarta.ejb.Remote;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PermisoDto;

@Remote
public interface PermisoEJBRemote {
    PermisoDto getPermisoById(Long id);
    PaginaDto<PermisoDto> getAll(Paginacion paginacion);
    PaginaDto<PermisoDto> getByVehiculo(Long idVehiculo, Paginacion paginacion);
    PermisoDto getVigente(Long idVehiculo, LocalDate fecha);
    void addPermiso(PermisoDto permisoDto);
    void updatePermiso(Long id, PermisoDto permisoDto);
    void deletePermiso(Long id);
}
