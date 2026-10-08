package tse.cargauy.negocio.vehiculo;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.EJB;
import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.entities.Rol;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.empresa.EmpresaEJBLocal;

@Stateless
@RolesAllowed({ Rol.RESPONSABLE, Rol.FUNCIONARIO })
public class VehiculoEJB implements VehiculoEJBLocal, VehiculoEJBRemote {

    @EJB
    VehiculoDAOLocal vehiculoDAO;

    @EJB
    EmpresaDAOLocal empresaDAO;

    @EJB
    EmpresaEJBLocal empresaEJB;

    public VehiculoDto getVehiculoById(Long id) {
        VehiculoDto vehiculo = vehiculoDAO.getVehiculoById(id);
        if (vehiculo != null) {
            empresaEJB.validarAcceso(vehiculo.getIdEmpresa());
        }
        return vehiculo;
    }

    @RolesAllowed(Rol.FUNCIONARIO)
    public PaginaDto<VehiculoDto> getAll(Paginacion paginacion) {
        return vehiculoDAO.getAll(paginacion);
    }

    public PaginaDto<VehiculoDto> getByEmpresa(Long idEmpresa, Paginacion paginacion) {
        empresaEJB.validarAcceso(idEmpresa);
        return vehiculoDAO.getByEmpresa(idEmpresa, paginacion);
    }

    @RolesAllowed(Rol.RESPONSABLE)
    public void addVehiculo(VehiculoDto vehiculoDto) {
        validar(vehiculoDto);
        empresaEJB.validarAcceso(vehiculoDto.getIdEmpresa());
        if (vehiculoDAO.getVehiculoByMatricula(vehiculoDto.getMatricula()) != null) {
            throw new CargaUYException(CodigoError.VEHICULO_MATRICULA_DUPLICADA, vehiculoDto.getMatricula());
        }

        vehiculoDAO.addVehiculo(vehiculoDto);
    }

    @RolesAllowed(Rol.RESPONSABLE)
    public void updateVehiculo(Long id, VehiculoDto vehiculoDto) {
        validarAccesoAlVehiculo(id);
        validar(vehiculoDto);
        empresaEJB.validarAcceso(vehiculoDto.getIdEmpresa());
        VehiculoDto existente = vehiculoDAO.getVehiculoByMatricula(vehiculoDto.getMatricula());
        if (existente != null && !existente.getId().equals(id)) {
            throw new CargaUYException(CodigoError.VEHICULO_MATRICULA_DUPLICADA, vehiculoDto.getMatricula());
        }

        vehiculoDAO.updateVehiculo(id, vehiculoDto);
    }

    @RolesAllowed(Rol.RESPONSABLE)
    public void deleteVehiculo(Long id) {
        validarAccesoAlVehiculo(id);
        vehiculoDAO.deleteVehiculo(id);
    }

    private void validarAccesoAlVehiculo(Long id) {
        VehiculoDto existente = vehiculoDAO.getVehiculoById(id);
        if (existente != null) {
            empresaEJB.validarAcceso(existente.getIdEmpresa());
        }
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
