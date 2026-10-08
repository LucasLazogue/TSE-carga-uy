package tse.cargauy.web.beans;

import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.VehiculoDto;
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

    @PostConstruct
    public void init() {
        empresas = empresaEJB.getAll(new Paginacion(0, Paginacion.TAMANIO_MAXIMO)).getItems();
        getAllVehiculos();
    }

    public void getAllVehiculos() {
        idEmpresa = null;
        vehiculos = vehiculoEJB.getAll(new Paginacion(0, Paginacion.TAMANIO_MAXIMO)).getItems();
    }

    public void findVehiculos() {
        if (idEmpresa == null) {
            vehiculos = vehiculoEJB.getAll(new Paginacion(0, Paginacion.TAMANIO_MAXIMO)).getItems();
        } else {
            vehiculos = vehiculoEJB.getByEmpresa(idEmpresa, new Paginacion(0, Paginacion.TAMANIO_MAXIMO)).getItems();
        }
    }

    public void createVehiculo() {
        vehiculoEJB.addVehiculo(vehiculo);
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Vehiculo guardado.", null));
        vehiculo = new VehiculoDto();
    }

    public List<VehiculoDto> getVehiculos() {
        return vehiculos;
    }

    public List<EmpresaDto> getEmpresas() {
        return empresas;
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
