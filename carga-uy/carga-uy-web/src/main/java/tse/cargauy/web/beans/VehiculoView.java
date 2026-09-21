package tse.cargauy.web.beans;

import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.negocio.empresa.EmpresaEJBLocal;
import tse.cargauy.negocio.vehiculo.VehiculoEJBLocal;

@Named
@ViewScoped
public class VehiculoView implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private VehiculoEJBLocal vehiculoEJB;

    @EJB
    private EmpresaEJBLocal empresaEJB;

    List<VehiculoDto> vehiculos;
    List<EmpresaDto> empresas;
    VehiculoDto vehiculo = new VehiculoDto();
    Long idEmpresa;
    String error;
    String mensaje;

    @PostConstruct
    public void init() {
        empresas = empresaEJB.getAll();
        getAllVehiculos();
    }

    public void getAllVehiculos() {
        idEmpresa = null;
        vehiculos = vehiculoEJB.getAll();
    }

    public void findVehiculos() {
        if (idEmpresa == null) {
            vehiculos = vehiculoEJB.getAll();
        } else {
            vehiculos = vehiculoEJB.getByEmpresa(idEmpresa);
        }
    }

    public void createVehiculo() {
        error = null;
        mensaje = null;
        try {
            vehiculoEJB.addVehiculo(vehiculo);
            mensaje = "Vehiculo guardado.";
            vehiculo = new VehiculoDto();
        } catch (CargaUYException e) {
            error = "Error al crear el vehiculo: " + e.getMessage();
        }
    }

    public List<VehiculoDto> getVehiculos() {
        return vehiculos;
    }

    public List<EmpresaDto> getEmpresas() {
        return empresas;
    }

    public String getError() {
        return error;
    }

    public String getMensaje() {
        return mensaje;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public void setVehiculo(VehiculoDto vehiculo) {
        this.vehiculo = vehiculo;
    }

    public VehiculoDto getVehiculo() {
        return vehiculo;
    }
}
