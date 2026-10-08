package tse.cargauy.negocio.permiso;

import java.time.LocalDate;
import java.util.List;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.EJB;
import tse.cargauy.data.permiso.PermisoDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.entities.Rol;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.empresa.EmpresaEJBLocal;

@Stateless
@RolesAllowed({ Rol.RESPONSABLE, Rol.FUNCIONARIO })
public class PermisoEJB implements PermisoEJBLocal, PermisoEJBRemote {

    @EJB
    PermisoDAOLocal permisoDAO;

    @EJB
    VehiculoDAOLocal vehiculoDAO;

    @EJB
    EmpresaEJBLocal empresaEJB;

    public PermisoDto getPermisoById(Long id) {
        PermisoDto permiso = permisoDAO.getPermisoById(id);
        if (permiso != null) {
            validarAccesoAlVehiculo(permiso.getIdVehiculo());
        }
        return permiso;
    }

    @RolesAllowed(Rol.FUNCIONARIO)
    public List<PermisoDto> getAll() {
        return permisoDAO.getAll();
    }

    public List<PermisoDto> getByVehiculo(Long idVehiculo) {
        validarAccesoAlVehiculo(idVehiculo);
        return permisoDAO.getByVehiculo(idVehiculo);
    }

    public PermisoDto getVigente(Long idVehiculo, LocalDate fecha) {
        validarAccesoAlVehiculo(idVehiculo);
        return permisoDAO.getVigente(idVehiculo, fecha);
    }

    @RolesAllowed(Rol.RESPONSABLE)
    public void addPermiso(PermisoDto permisoDto) {
        validar(permisoDto);
        validarAccesoAlVehiculo(permisoDto.getIdVehiculo());
        if (permisoDAO.getPermisoByNro(permisoDto.getNroPermiso()) != null) {
            throw new CargaUYException(CodigoError.PERMISO_NRO_DUPLICADO, permisoDto.getNroPermiso());
        }
        validarSuperposicion(permisoDto, null);

        permisoDAO.addPermiso(permisoDto);
    }

    @RolesAllowed(Rol.RESPONSABLE)
    public void updatePermiso(Long id, PermisoDto permisoDto) {
        validarAccesoAlPermiso(id);
        validar(permisoDto);
        validarAccesoAlVehiculo(permisoDto.getIdVehiculo());
        PermisoDto existente = permisoDAO.getPermisoByNro(permisoDto.getNroPermiso());
        if (existente != null && !existente.getId().equals(id)) {
            throw new CargaUYException(CodigoError.PERMISO_NRO_DUPLICADO, permisoDto.getNroPermiso());
        }
        validarSuperposicion(permisoDto, id);

        permisoDAO.updatePermiso(id, permisoDto);
    }

    @RolesAllowed(Rol.RESPONSABLE)
    public void deletePermiso(Long id) {
        validarAccesoAlPermiso(id);
        permisoDAO.deletePermiso(id);
    }

    private void validarAccesoAlPermiso(Long id) {
        PermisoDto existente = permisoDAO.getPermisoById(id);
        if (existente != null) {
            validarAccesoAlVehiculo(existente.getIdVehiculo());
        }
    }

    private void validarAccesoAlVehiculo(Long idVehiculo) {
        VehiculoDto vehiculo = vehiculoDAO.getVehiculoById(idVehiculo);
        if (vehiculo != null) {
            empresaEJB.validarAcceso(vehiculo.getIdEmpresa());
        }
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
