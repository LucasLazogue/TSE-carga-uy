package tse.cargauy.data.permiso;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.ejb.Local;
import tse.cargauy.dtos.FiltroPermisos;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PermisoDto;

@Local
public interface PermisoDAOLocal {
    PermisoDto getPermisoById(Long id);
    PermisoDto getPermisoByNro(String nroPermiso);
    PaginaDto<PermisoDto> getAll(Paginacion paginacion);
    PaginaDto<PermisoDto> getAll(FiltroPermisos filtro, Paginacion paginacion);
    PaginaDto<PermisoDto> getByVehiculo(Long idVehiculo, Paginacion paginacion);
    PermisoDto getSuperpuesto(Long idVehiculo, LocalDate desde, LocalDate hasta, Long idExcluir);
    PermisoDto getVigente(Long idVehiculo, LocalDate fecha);
    void addPermiso(PermisoDto permisoDto);
    void updatePermiso(Long id, PermisoDto permisoDto);
    void aprobar(Long id, LocalDate desde, LocalDate hasta, Long idFuncionario, LocalDateTime fecha);
    void rechazar(Long id, String motivo, Long idFuncionario, LocalDateTime fecha);
    void deletePermiso(Long id);
}
