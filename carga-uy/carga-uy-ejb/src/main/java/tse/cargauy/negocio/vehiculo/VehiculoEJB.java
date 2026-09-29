package tse.cargauy.negocio.vehiculo;

import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.ejb.EJB;
import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@Stateless
public class VehiculoEJB implements VehiculoEJBLocal, VehiculoEJBRemote {

    @EJB
    VehiculoDAOLocal vehiculoDAO;

    @EJB
    EmpresaDAOLocal empresaDAO;

    public VehiculoDto getVehiculoById(Long id) {
        return vehiculoDAO.getVehiculoById(id);
    }

    public List<VehiculoDto> getAll() {
        return vehiculoDAO.getAll();
    }

    public List<VehiculoDto> getByEmpresa(Long idEmpresa) {
        return vehiculoDAO.getByEmpresa(idEmpresa);
    }

    public void addVehiculo(VehiculoDto vehiculoDto) {
        validar(vehiculoDto);
        if (vehiculoDAO.getVehiculoByMatricula(vehiculoDto.getMatricula()) != null) {
            throw new CargaUYException(CodigoError.VEHICULO_MATRICULA_DUPLICADA, vehiculoDto.getMatricula());
        }

        vehiculoDAO.addVehiculo(vehiculoDto);
    }

    public void updateVehiculo(Long id, VehiculoDto vehiculoDto) {
        validar(vehiculoDto);
        VehiculoDto existente = vehiculoDAO.getVehiculoByMatricula(vehiculoDto.getMatricula());
        if (existente != null && !existente.getId().equals(id)) {
            throw new CargaUYException(CodigoError.VEHICULO_MATRICULA_DUPLICADA, vehiculoDto.getMatricula());
        }

        vehiculoDAO.updateVehiculo(id, vehiculoDto);
    }

    public void deleteVehiculo(Long id) {
        vehiculoDAO.deleteVehiculo(id);
    }

    private void validar(VehiculoDto vehiculoDto) {
        if (vehiculoDto.getMatricula() == null || vehiculoDto.getMatricula().isBlank()) {
            throw new CargaUYException(CodigoError.VEHICULO_MATRICULA_REQUERIDA);
        }
        if (vehiculoDto.getMarca() == null || vehiculoDto.getMarca().isBlank()) {
            throw new CargaUYException(CodigoError.VEHICULO_MARCA_REQUERIDA);
        }
        if (vehiculoDto.getPesoVehiculo() <= 0) {
            throw new CargaUYException(CodigoError.VEHICULO_PESO_INVALIDO);
        }
        if (vehiculoDto.getCapacidadCarga() <= 0) {
            throw new CargaUYException(CodigoError.VEHICULO_CAPACIDAD_INVALIDA);
        }
        if (vehiculoDto.getIdEmpresa() == null) {
            throw new CargaUYException(CodigoError.VEHICULO_EMPRESA_REQUERIDA);
        }
        if (empresaDAO.getEmpresaById(vehiculoDto.getIdEmpresa()) == null) {
            throw new CargaUYException(CodigoError.EMPRESA_NO_ENCONTRADA, String.valueOf(vehiculoDto.getIdEmpresa()));
        }
    }

}
