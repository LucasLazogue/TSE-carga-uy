package tse.cargauy.negocio.viaje;

import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.ejb.EJB;
import tse.cargauy.data.guia.GuiaDAOLocal;
import tse.cargauy.data.permiso.PermisoDAOLocal;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.data.viaje.ViajeDAOLocal;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.entities.Rol;
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

    public ViajeDto getViajeById(Long id) {
        return viajeDAO.getViajeById(id);
    }

    public ViajeDto getByGuia(Long idGuia) {
        return viajeDAO.getByGuia(idGuia);
    }

    public List<ViajeDto> getAll() {
        return viajeDAO.getAll();
    }

    public List<ViajeDto> getByEmpresa(Long idEmpresa) {
        return viajeDAO.getByEmpresa(idEmpresa);
    }

    public List<ViajeDto> getByChofer(Long idChofer) {
        return viajeDAO.getByChofer(idChofer);
    }

    public List<ViajeDto> getByVehiculo(Long idVehiculo) {
        return viajeDAO.getByVehiculo(idVehiculo);
    }

    public ViajeDto asignarViaje(ViajeDto viajeDto) {
        if (viajeDto.getIdGuia() == null) {
            throw new CargaUYException(CodigoError.VIAJE_GUIA_REQUERIDA);
        }
        GuiaDto guia = guiaDAO.getGuiaById(viajeDto.getIdGuia());
        if (guia == null) {
            throw new CargaUYException(CodigoError.GUIA_NO_ENCONTRADA, String.valueOf(viajeDto.getIdGuia()));
        }
        if (viajeDAO.getByGuia(guia.getId()) != null) {
            throw new CargaUYException(CodigoError.VIAJE_GUIA_YA_ASIGNADA, guia.getNroGuia());
        }
        validarAsignacion(viajeDto, guia);

        return viajeDAO.addViaje(viajeDto);
    }

    public void reasignarViaje(Long id, ViajeDto viajeDto) {
        ViajeDto actual = getModificable(id);
        validarAsignacion(viajeDto, guiaDAO.getGuiaById(actual.getIdGuia()));

        viajeDAO.updateViaje(id, viajeDto);
    }

    public void deleteViaje(Long id) {
        getModificable(id);

        viajeDAO.deleteViaje(id);
    }

    private ViajeDto getModificable(Long id) {
        ViajeDto viaje = viajeDAO.getViajeById(id);
        if (viaje == null) {
            throw new CargaUYException(CodigoError.VIAJE_NO_ENCONTRADO, String.valueOf(id));
        }
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
        UsuarioDto chofer = usuarioDAO.getById(viajeDto.getIdChofer());
        if (chofer == null || !chofer.getRoles().contains(Rol.CHOFER)) {
            throw new CargaUYException(CodigoError.VIAJE_CHOFER_INVALIDO, String.valueOf(viajeDto.getIdChofer()));
        }
    }

}
