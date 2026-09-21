package tse.cargauy.negocio.vehiculo;

import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.ejb.EJB;
import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.exceptions.CargaUYException;

@Stateless
public class VehiculoEJB implements VehiculoEJBLocal, VehiculoEJBRemote {

    @EJB
    private VehiculoDAOLocal vehiculoDAO;

    @EJB
    private EmpresaDAOLocal empresaDAO;

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
            throw new CargaUYException("Ya existe un vehiculo con la matricula " + vehiculoDto.getMatricula() + ".");
        }

        vehiculoDAO.addVehiculo(vehiculoDto);
    }

    public void updateVehiculo(Long id, VehiculoDto vehiculoDto) {
        validar(vehiculoDto);
        VehiculoDto existente = vehiculoDAO.getVehiculoByMatricula(vehiculoDto.getMatricula());
        if (existente != null && !existente.getId().equals(id)) {
            throw new CargaUYException("Ya existe un vehiculo con la matricula " + vehiculoDto.getMatricula() + ".");
        }

        vehiculoDAO.updateVehiculo(id, vehiculoDto);
    }

    public void deleteVehiculo(Long id) {
        vehiculoDAO.deleteVehiculo(id);
    }

    private void validar(VehiculoDto vehiculoDto) {
        if (vehiculoDto.getMatricula() == null || vehiculoDto.getMatricula().isBlank()) {
            throw new CargaUYException("La matricula es obligatoria.");
        }
        if (vehiculoDto.getMarca() == null || vehiculoDto.getMarca().isBlank()) {
            throw new CargaUYException("La marca es obligatoria.");
        }
        if (vehiculoDto.getPesoVehiculo() <= 0) {
            throw new CargaUYException("El peso del vehiculo debe ser mayor a 0.");
        }
        if (vehiculoDto.getCapacidadCarga() <= 0) {
            throw new CargaUYException("La capacidad de carga debe ser mayor a 0.");
        }
        if (vehiculoDto.getIdEmpresa() == null || empresaDAO.getEmpresaById(vehiculoDto.getIdEmpresa()) == null) {
            throw new CargaUYException("La empresa indicada no existe.");
        }
    }

}
