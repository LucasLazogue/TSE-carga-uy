package tse.cargauy.negocio.permiso;

import java.time.LocalDate;
import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.ejb.EJB;
import tse.cargauy.data.permiso.PermisoDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.exceptions.CargaUYException;

@Stateless
public class PermisoEJB implements PermisoEJBLocal, PermisoEJBRemote {

    @EJB
    private PermisoDAOLocal permisoDAO;

    @EJB
    private VehiculoDAOLocal vehiculoDAO;

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
            throw new CargaUYException("Ya existe un permiso con el numero " + permisoDto.getNroPermiso() + ".");
        }
        validarSuperposicion(permisoDto, null);

        permisoDAO.addPermiso(permisoDto);
    }

    public void updatePermiso(Long id, PermisoDto permisoDto) {
        validar(permisoDto);
        PermisoDto existente = permisoDAO.getPermisoByNro(permisoDto.getNroPermiso());
        if (existente != null && !existente.getId().equals(id)) {
            throw new CargaUYException("Ya existe un permiso con el numero " + permisoDto.getNroPermiso() + ".");
        }
        validarSuperposicion(permisoDto, id);

        permisoDAO.updatePermiso(id, permisoDto);
    }

    public void deletePermiso(Long id) {
        permisoDAO.deletePermiso(id);
    }

    private void validar(PermisoDto permisoDto) {
        if (permisoDto.getNroPermiso() == null || permisoDto.getNroPermiso().isBlank()) {
            throw new CargaUYException("El numero de permiso es obligatorio.");
        }
        if (permisoDto.getValidoDesde() == null || permisoDto.getValidoHasta() == null) {
            throw new CargaUYException("El periodo de validez es obligatorio.");
        }
        if (permisoDto.getValidoHasta().isBefore(permisoDto.getValidoDesde())) {
            throw new CargaUYException("La fecha de fin de validez no puede ser anterior a la de inicio.");
        }
        if (permisoDto.getIdVehiculo() == null || vehiculoDAO.getVehiculoById(permisoDto.getIdVehiculo()) == null) {
            throw new CargaUYException("El vehiculo indicado no existe.");
        }
    }

    private void validarSuperposicion(PermisoDto permisoDto, Long id) {
        for (PermisoDto existente : permisoDAO.getByVehiculo(permisoDto.getIdVehiculo())) {
            if (existente.getId().equals(id)) {
                continue;
            }
            if (!permisoDto.getValidoDesde().isAfter(existente.getValidoHasta())
                    && !permisoDto.getValidoHasta().isBefore(existente.getValidoDesde())) {
                throw new CargaUYException("El vehiculo ya tiene el permiso " + existente.getNroPermiso() +
                        " valido entre " + existente.getValidoDesde() + " y " + existente.getValidoHasta() + ".");
            }
        }
    }

}
