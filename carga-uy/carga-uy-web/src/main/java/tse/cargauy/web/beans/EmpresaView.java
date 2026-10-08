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
import tse.cargauy.negocio.empresa.EmpresaEJBLocal;

@Named
@ViewScoped
public class EmpresaView implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private EmpresaEJBLocal empresaEJB;

    List<EmpresaDto> empresas;
    EmpresaDto empresa = new EmpresaDto();
    String nombre;

    @PostConstruct
    public void init() {
        getAllEmpresas();
    }

    public void getAllEmpresas() {
        nombre = null;
        empresas = empresaEJB.getAll(new Paginacion(0, Paginacion.TAMANIO_MAXIMO)).getItems();
    }

    public void findEmpresas() {
        empresas = empresaEJB.findByNombre(nombre, new Paginacion(0, Paginacion.TAMANIO_MAXIMO)).getItems();
    }

    public void createEmpresa() {
        empresaEJB.addEmpresa(empresa);
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Empresa guardada.", null));
        empresa = new EmpresaDto();
    }

    public List<EmpresaDto> getEmpresas() {
        return empresas;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setEmpresa(EmpresaDto empresa) {
        this.empresa = empresa;
    }

    public EmpresaDto getEmpresa() {
        return empresa;
    }
}
