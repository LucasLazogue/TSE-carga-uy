package tse.cargauy.negocio.permiso;

import java.time.LocalDate;
import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.ejb.EJB;
import tse.cargauy.data.permiso.PermisoDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@Stateless
public class PermisoEJB implements PermisoEJBLocal, PermisoEJBRemote {

    @EJB
    PermisoDAOLocal permisoDAO;

    @EJB
    VehiculoDAOLocal vehiculoDAO;

    public PermisoDto getPermisoById(Long id) {
        return permisoDAO.getPermisoById(id);
    }

    public List<PermisoDto> getAll() {
        return permisoDAO.getAll();
    }

    public List<PermisoDto> getByVehiculo(Long idVehiculo) {
        return permisoDAO.getByVehiculo(idVehiculo);
    }

    public PermisoDto getVigente(Long idVehiculo, LocalDate fecha) {
        return permisoDAO.getVigente(idVehiculo, fecha);
    }

    public void addPermiso(PermisoDto permisoDto) {
        validar(permisoDto);
        if (permisoDAO.getPermisoByNro(permisoDto.getNroPermiso()) != null) {
            throw new CargaUYException(CodigoError.PERMISO_NRO_DUPLICADO, permisoDto.getNroPermiso());
        }
        validarSuperposicion(permisoDto, null);

        permisoDAO.addPermiso(permisoDto);
    }

    public void updatePermiso(Long id, PermisoDto permisoDto) {
        validar(permisoDto);
        PermisoDto existente = permisoDAO.getPermisoByNro(permisoDto.getNroPermiso());
        if (existente != null && !existente.getId().equals(id)) {
            throw new CargaUYException(CodigoError.PERMISO_NRO_DUPLICADO, permisoDto.getNroPermiso());
        }
        validarSuperposicion(permisoDto, id);

        permisoDAO.updatePermiso(id, permisoDto);
    }

    public void deletePermiso(Long id) {
        permisoDAO.deletePermiso(id);
    }

    private void validar(PermisoDto permisoDto) {
        if (permisoDto.getNroPermiso() == null || permisoDto.getNroPermiso().isBlank()) {
            throw new CargaUYException(CodigoError.PERMISO_NRO_REQUERIDO);
        }
        if (permisoDto.getValidoDesde() == null || permisoDto.getValidoHasta() == null) {
            throw new CargaUYException(CodigoError.PERMISO_PERIODO_REQUERIDO);
        }
        if (permisoDto.getValidoHasta().isBefore(permisoDto.getValidoDesde())) {
            throw new CargaUYException(CodigoError.PERMISO_RANGO_FECHAS_INVALIDO);
        }
        if (permisoDto.getIdVehiculo() == null) {
            throw new CargaUYException(CodigoError.PERMISO_VEHICULO_REQUERIDO);
        }
        if (vehiculoDAO.getVehiculoById(permisoDto.getIdVehiculo()) == null) {
            throw new CargaUYException(CodigoError.VEHICULO_NO_ENCONTRADO, String.valueOf(permisoDto.getIdVehiculo()));
        }
    }

    private void validarSuperposicion(PermisoDto permisoDto, Long id) {
        for (PermisoDto existente : permisoDAO.getByVehiculo(permisoDto.getIdVehiculo())) {
            if (existente.getId().equals(id)) {
                continue;
            }
            if (!permisoDto.getValidoDesde().isAfter(existente.getValidoHasta())
                    && !permisoDto.getValidoHasta().isBefore(existente.getValidoDesde())) {
                throw new CargaUYException(CodigoError.PERMISO_SUPERPUESTO,
                        existente.getNroPermiso(),
                        existente.getValidoDesde().toString(),
                        existente.getValidoHasta().toString());
            }
        }
    }

}
