package tse.cargauy.negocio.itv;

import java.util.List;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import tse.cargauy.data.itv.ItvDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.dtos.ItvDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.entities.Rol;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.empresa.EmpresaEJBLocal;

@Stateless
@RolesAllowed({ Rol.RESPONSABLE, Rol.FUNCIONARIO })
public class ItvEJB implements ItvEJBLocal {

    @EJB
    ItvDAOLocal itvDAO;

    @EJB
    VehiculoDAOLocal vehiculoDAO;

    @EJB
    EmpresaEJBLocal empresaEJB;

    public List<ItvDto> listar(Long idVehiculo) {
        validarAccesoAlVehiculo(idVehiculo);
        return itvDAO.getByVehiculo(idVehiculo);
    }

    @RolesAllowed(Rol.RESPONSABLE)
    public void agregar(Long idVehiculo, ItvDto itv) {
        validarAccesoAlVehiculo(idVehiculo);
        if (itv.getFechaInspeccion() == null || itv.getFechaVencimiento() == null) {
            throw new CargaUYException(CodigoError.ITV_FECHAS_REQUERIDAS);
        }
        if (itv.getFechaVencimiento().isBefore(itv.getFechaInspeccion())) {
            throw new CargaUYException(CodigoError.ITV_RANGO_FECHAS_INVALIDO);
        }

        itvDAO.addItv(new ItvDto(idVehiculo, itv.getFechaInspeccion(), itv.getFechaVencimiento()));
    }

    @RolesAllowed(Rol.RESPONSABLE)
    public void eliminar(Long idVehiculo, Long id) {
        validarAccesoAlVehiculo(idVehiculo);
        ItvDto itv = itvDAO.getItvById(id);
        if (itv == null || !itv.getIdVehiculo().equals(idVehiculo)) {
            throw new CargaUYException(CodigoError.ITV_NO_ENCONTRADA, String.valueOf(id));
        }

        itvDAO.deleteItv(id);
    }

    private void validarAccesoAlVehiculo(Long idVehiculo) {
        VehiculoDto vehiculo = vehiculoDAO.getVehiculoById(idVehiculo);
        if (vehiculo == null) {
            throw new CargaUYException(CodigoError.VEHICULO_NO_ENCONTRADO, String.valueOf(idVehiculo));
        }
        empresaEJB.validarAcceso(vehiculo.getIdEmpresa());
    }

}
