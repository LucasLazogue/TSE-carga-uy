package tse.cargauy.negocio.permiso;

import java.time.LocalDate;
import jakarta.ejb.Local;
import tse.cargauy.dtos.AprobacionPermisoDto;
import tse.cargauy.dtos.FiltroPermisos;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.RechazoPermisoDto;
import tse.cargauy.dtos.SolicitudPermisoDto;

@Local
public interface PermisoEJBLocal {
    PermisoDto getPermisoById(Long id);
    PaginaDto<PermisoDto> getAll(Paginacion paginacion);
    PaginaDto<PermisoDto> getByVehiculo(Long idVehiculo, Paginacion paginacion);
    PaginaDto<PermisoDto> listar(FiltroPermisos filtro, Paginacion paginacion);
    PermisoDto getVigente(Long idVehiculo, LocalDate fecha);
    void addPermiso(SolicitudPermisoDto solicitud);
    void updatePermiso(Long id, SolicitudPermisoDto solicitud);
    void deletePermiso(Long id);
    void aprobar(Long id, AprobacionPermisoDto aprobacion);
    void rechazar(Long id, RechazoPermisoDto rechazo);
}
