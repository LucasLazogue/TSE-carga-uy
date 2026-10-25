package tse.cargauy.negocio.permiso;

import java.time.LocalDate;
import jakarta.ejb.Remote;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.SolicitudPermisoDto;

@Remote
public interface PermisoEJBRemote {
    PermisoDto getPermisoById(Long id);
    PaginaDto<PermisoDto> getAll(Paginacion paginacion);
    PaginaDto<PermisoDto> getByVehiculo(Long idVehiculo, Paginacion paginacion);
    PermisoDto getVigente(Long idVehiculo, LocalDate fecha);
    void addPermiso(SolicitudPermisoDto solicitud);
    void updatePermiso(Long id, SolicitudPermisoDto solicitud);
    void deletePermiso(Long id);
}
