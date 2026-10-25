package tse.cargauy.negocio.permiso;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.EJB;
import tse.cargauy.data.permiso.PermisoDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.dtos.AprobacionPermisoDto;
import tse.cargauy.dtos.FiltroPermisos;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.RechazoPermisoDto;
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

    @Resource
    SessionContext contexto;

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

    public PaginaDto<PermisoDto> listar(FiltroPermisos filtro, Paginacion paginacion) {
        if (filtro.getIdVehiculo() != null) {
            validarAccesoAlVehiculo(filtro.getIdVehiculo());
        } else if (!contexto.isCallerInRole(Rol.FUNCIONARIO)) {
            throw new CargaUYException(CodigoError.ACCESO_DENEGADO);
        }
        return permisoDAO.getAll(filtro, paginacion);
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

    @RolesAllowed(Rol.FUNCIONARIO)
    public void aprobar(Long id, AprobacionPermisoDto aprobacion) {
        PermisoDto permiso = buscarPendiente(id);
        LocalDate desde = aprobacion.getValidoDesde() == null ? permiso.getValidoDesde() : aprobacion.getValidoDesde();
        LocalDate hasta = aprobacion.getValidoHasta();
        if (hasta == null) {
            throw new CargaUYException(CodigoError.PERMISO_FIN_REQUERIDO);
        }
        if (hasta.isBefore(desde)) {
            throw new CargaUYException(CodigoError.PERMISO_RANGO_FECHAS_INVALIDO);
        }
        LocalDateTime ahora = ahora();
        if (hasta.isBefore(ahora.toLocalDate())) {
            throw new CargaUYException(CodigoError.PERMISO_VENCIDO);
        }
        PermisoDto superpuesto = permisoDAO.getSuperpuesto(permiso.getIdVehiculo(), desde, hasta, id);
        if (superpuesto != null) {
            throw new CargaUYException(CodigoError.PERMISO_SUPERPUESTO, superpuesto.getNroPermiso(),
                    superpuesto.getValidoDesde().toString(), superpuesto.getValidoHasta().toString());
        }

        permisoDAO.aprobar(id, desde, hasta, idUsuario(), ahora);
    }

    @RolesAllowed(Rol.FUNCIONARIO)
    public void rechazar(Long id, RechazoPermisoDto rechazo) {
        buscarPendiente(id);
        if (rechazo.getMotivo() == null || rechazo.getMotivo().isBlank()) {
            throw new CargaUYException(CodigoError.PERMISO_MOTIVO_REQUERIDO);
        }

        permisoDAO.rechazar(id, rechazo.getMotivo().strip(), idUsuario(), ahora());
    }

    private PermisoDto buscarPendiente(Long id) {
        PermisoDto permiso = permisoDAO.getPermisoById(id);
        if (permiso == null) {
            throw new CargaUYException(CodigoError.PERMISO_NO_ENCONTRADO, String.valueOf(id));
        }
        if (permiso.getEstado() != EstadoPermiso.PENDIENTE) {
            throw new CargaUYException(CodigoError.PERMISO_NO_PENDIENTE, permiso.getNroPermiso());
        }
        return permiso;
    }

    Long idUsuario() {
        return Long.valueOf(contexto.getCallerPrincipal().getName());
    }

    LocalDateTime ahora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
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
