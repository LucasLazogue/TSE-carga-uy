package tse.cargauy.negocio.viaje;

import java.util.List;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import tse.cargauy.data.guia.GuiaDAOLocal;
import tse.cargauy.data.permiso.PermisoDAOLocal;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.data.viaje.ViajeDAOLocal;
import tse.cargauy.dtos.FiltroViajes;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@Stateless
public class ViajeEJB implements ViajeEJBLocal, ViajeEJBRemote {

    @EJB
    ViajeDAOLocal viajeDAO;

    @EJB
    GuiaDAOLocal guiaDAO;

    @EJB
    VehiculoDAOLocal vehiculoDAO;

    @EJB
    PermisoDAOLocal permisoDAO;

    @EJB
    UsuarioDAOLocal usuarioDAO;

    public PaginaDto<ViajeDto> listar(Long idEmpresa, FiltroViajes filtro, Paginacion paginacion) {
        filtro.setIdsEmpresa(List.of(idEmpresa));
        return viajeDAO.getAll(filtro, paginacion);
    }

    public ViajeDto getViaje(Long idEmpresa, Long id) {
        return getDeEmpresa(idEmpresa, id);
    }

    public ViajeDto asignarViaje(Long idEmpresa, ViajeDto viajeDto) {
        if (viajeDto.getIdGuia() == null) {
            throw new CargaUYException(CodigoError.VIAJE_GUIA_REQUERIDA);
        }
        GuiaDto guia = guiaDAO.getGuiaById(viajeDto.getIdGuia());
        if (guia == null || !guia.getIdEmpresa().equals(idEmpresa)) {
            throw new CargaUYException(CodigoError.GUIA_NO_ENCONTRADA, String.valueOf(viajeDto.getIdGuia()));
        }
        if (viajeDAO.getByGuia(guia.getId()) != null) {
            throw new CargaUYException(CodigoError.VIAJE_GUIA_YA_ASIGNADA, guia.getNroGuia());
        }
        validarAsignacion(viajeDto, guia);

        return viajeDAO.addViaje(viajeDto);
    }

    public ViajeDto reasignarViaje(Long idEmpresa, Long id, ViajeDto viajeDto) {
        ViajeDto actual = getModificable(idEmpresa, id);
        validarAsignacion(viajeDto, guiaDAO.getGuiaById(actual.getIdGuia()));

        viajeDAO.updateViaje(id, viajeDto);
        return viajeDAO.getViajeById(id);
    }

    public void deleteViaje(Long idEmpresa, Long id) {
        getModificable(idEmpresa, id);

        viajeDAO.deleteViaje(id);
    }

    private ViajeDto getDeEmpresa(Long idEmpresa, Long id) {
        ViajeDto viaje = viajeDAO.getViajeById(id);
        if (viaje == null || !guiaDAO.getGuiaById(viaje.getIdGuia()).getIdEmpresa().equals(idEmpresa)) {
            throw new CargaUYException(CodigoError.VIAJE_NO_ENCONTRADO, String.valueOf(id));
        }
        return viaje;
    }

    private ViajeDto getModificable(Long idEmpresa, Long id) {
        ViajeDto viaje = getDeEmpresa(idEmpresa, id);
        if (viaje.getEstado() != EstadoViaje.ASIGNADO) {
            throw new CargaUYException(CodigoError.VIAJE_NO_MODIFICABLE, String.valueOf(id));
        }
        return viaje;
    }

    private void validarAsignacion(ViajeDto viajeDto, GuiaDto guia) {
        if (viajeDto.getIdVehiculo() == null) {
            throw new CargaUYException(CodigoError.VIAJE_VEHICULO_REQUERIDO);
        }
        if (viajeDto.getIdChofer() == null) {
            throw new CargaUYException(CodigoError.VIAJE_CHOFER_REQUERIDO);
        }
        VehiculoDto vehiculo = vehiculoDAO.getVehiculoById(viajeDto.getIdVehiculo());
        if (vehiculo == null) {
            throw new CargaUYException(CodigoError.VEHICULO_NO_ENCONTRADO, String.valueOf(viajeDto.getIdVehiculo()));
        }
        if (!vehiculo.getIdEmpresa().equals(guia.getIdEmpresa())) {
            throw new CargaUYException(CodigoError.VIAJE_VEHICULO_OTRA_EMPRESA, vehiculo.getMatricula());
        }
        if (guia.getVolumen() > vehiculo.getCapacidadCarga()) {
            throw new CargaUYException(CodigoError.VIAJE_CAPACIDAD_EXCEDIDA,
                    String.valueOf(guia.getVolumen()), String.valueOf(vehiculo.getCapacidadCarga()));
        }
        if (permisoDAO.getVigente(vehiculo.getId(), guia.getFecha()) == null) {
            throw new CargaUYException(CodigoError.VIAJE_VEHICULO_SIN_PERMISO, vehiculo.getMatricula(), guia.getFecha().toString());
        }
        if (usuarioDAO.getChoferVigente(viajeDto.getIdChofer(), guia.getIdEmpresa(), guia.getFecha()) == null) {
            throw new CargaUYException(CodigoError.VIAJE_CHOFER_INVALIDO, String.valueOf(viajeDto.getIdChofer()), guia.getFecha().toString());
        }
    }

}
