package tse.cargauy.web.beans;

import java.io.Serializable;
import java.util.List;

import org.omnifaces.cdi.ViewScoped;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.inject.Model;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.FiltroVehiculos;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.negocio.empresa.EmpresaEJBLocal;
import tse.cargauy.negocio.vehiculo.VehiculoEJBLocal;

// @Model es solo para que WildFly descubra el bean: dentro del EAR no reconoce como alcance la anotacion de
// OmniFaces, que viene en WEB-INF/lib. vale el alcance explicito (@ViewScoped), no el de pedido de @Model
@Model
@ViewScoped(saveInViewState = true)
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
            vehiculos = vehiculoEJB.getByEmpresa(new FiltroVehiculos(idEmpresa, null), new Paginacion(0, Paginacion.TAMANIO_MAXIMO)).getItems();
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
