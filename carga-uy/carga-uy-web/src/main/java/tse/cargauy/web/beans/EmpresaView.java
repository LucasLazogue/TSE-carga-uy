package tse.cargauy.web.beans;

import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.exceptions.CargaUYException;
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
    String error;
    String mensaje;

    @PostConstruct
    public void init() {
        getAllEmpresas();
    }

    public void getAllEmpresas() {
        nombre = null;
        empresas = empresaEJB.getAll();
    }

    public void findEmpresas() {
        empresas = empresaEJB.findByNombre(nombre);
    }

    public void createEmpresa() {
        error = null;
        mensaje = null;
        try {
            empresaEJB.addEmpresa(empresa);
            mensaje = "Empresa guardada.";
            empresa = new EmpresaDto();
        } catch (CargaUYException e) {
            error = "Error al crear la empresa: " + e.getMessage();
        }
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
