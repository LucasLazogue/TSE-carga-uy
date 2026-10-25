package tse.cargauy.negocio.permiso;

import java.time.LocalDate;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.EJB;
import tse.cargauy.data.permiso.PermisoDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.SolicitudPermisoDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.entities.EstadoPermiso;
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
    public PaginaDto<PermisoDto> getAll(Paginacion paginacion) {
        return permisoDAO.getAll(paginacion);
    }

    public PaginaDto<PermisoDto> getByVehiculo(Long idVehiculo, Paginacion paginacion) {
        validarAccesoAlVehiculo(idVehiculo);
        return permisoDAO.getByVehiculo(idVehiculo, paginacion);
    }

    public PermisoDto getVigente(Long idVehiculo, LocalDate fecha) {
        validarAccesoAlVehiculo(idVehiculo);
        return permisoDAO.getVigente(idVehiculo, fecha);
    }

    @RolesAllowed(Rol.RESPONSABLE)
    public void addPermiso(SolicitudPermisoDto solicitud) {
        validar(solicitud);
        validarAccesoAlVehiculo(solicitud.getIdVehiculo());
        if (permisoDAO.getPermisoByNro(solicitud.getNroPermiso()) != null) {
            throw new CargaUYException(CodigoError.PERMISO_NRO_DUPLICADO, solicitud.getNroPermiso());
        }

        permisoDAO.addPermiso(pendiente(solicitud));
    }

    @RolesAllowed(Rol.RESPONSABLE)
    public void updatePermiso(Long id, SolicitudPermisoDto solicitud) {
        validarModificable(id);
        validar(solicitud);
        validarAccesoAlVehiculo(solicitud.getIdVehiculo());
        PermisoDto existente = permisoDAO.getPermisoByNro(solicitud.getNroPermiso());
        if (existente != null && !existente.getId().equals(id)) {
            throw new CargaUYException(CodigoError.PERMISO_NRO_DUPLICADO, solicitud.getNroPermiso());
        }

        permisoDAO.updatePermiso(id, pendiente(solicitud));
    }

    @RolesAllowed(Rol.RESPONSABLE)
    public void deletePermiso(Long id) {
        validarModificable(id);
        permisoDAO.deletePermiso(id);
    }

    private static PermisoDto pendiente(SolicitudPermisoDto solicitud) {
        return new PermisoDto(solicitud.getNroPermiso(), solicitud.getValidoDesde(), null, solicitud.getIdVehiculo(),
                EstadoPermiso.PENDIENTE);
    }

    private void validarModificable(Long id) {
        PermisoDto existente = permisoDAO.getPermisoById(id);
        if (existente == null) {
            return;
        }
        validarAccesoAlVehiculo(existente.getIdVehiculo());
        if (existente.getEstado() == EstadoPermiso.APROBADO) {
            throw new CargaUYException(CodigoError.PERMISO_NO_MODIFICABLE, existente.getNroPermiso());
        }
    }

    private void validarAccesoAlVehiculo(Long idVehiculo) {
        VehiculoDto vehiculo = vehiculoDAO.getVehiculoById(idVehiculo);
        if (vehiculo != null) {
            empresaEJB.validarAcceso(vehiculo.getIdEmpresa());
        }
    }

    private void validar(SolicitudPermisoDto solicitud) {
        if (solicitud.getNroPermiso() == null || solicitud.getNroPermiso().isBlank()) {
            throw new CargaUYException(CodigoError.PERMISO_NRO_REQUERIDO);
        }
        if (solicitud.getValidoDesde() == null) {
            throw new CargaUYException(CodigoError.PERMISO_INICIO_REQUERIDO);
        }
        if (solicitud.getIdVehiculo() == null) {
            throw new CargaUYException(CodigoError.PERMISO_VEHICULO_REQUERIDO);
        }
        if (vehiculoDAO.getVehiculoById(solicitud.getIdVehiculo()) == null) {
            throw new CargaUYException(CodigoError.VEHICULO_NO_ENCONTRADO, String.valueOf(solicitud.getIdVehiculo()));
        }
    }

}
