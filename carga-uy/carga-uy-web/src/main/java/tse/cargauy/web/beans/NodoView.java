package tse.cargauy.web.beans;

import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.entities.EstadoNodo;
import tse.cargauy.entities.TipoNodo;
import tse.cargauy.negocio.nodo.NodoPerifericoEJBLocal;

// CU-19 en el backoffice. TODO restringir al rol administrador cuando exista (CU-03)
@Named
@ViewScoped
public class NodoView implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private NodoPerifericoEJBLocal nodoEJB;

    List<NodoPerifericoDto> nodos;
    NodoPerifericoDto nodo = new NodoPerifericoDto();

    @PostConstruct
    public void init() {
        cargar();
    }

    public void cargar() {
        nodos = nodoEJB.getVigentes();
    }

    public void createNodo() {
        nodoEJB.addNodo(nodo);
        mensaje("Nodo " + nodo.getIdentificador() + " registrado. Queda deshabilitado hasta que se habilite.");
        nodo = new NodoPerifericoDto();
    }

    public void habilitar(NodoPerifericoDto n) {
        nodoEJB.habilitar(n.getId());
        mensaje("Nodo " + n.getIdentificador() + " habilitado.");
        cargar();
    }

    public void deshabilitar(NodoPerifericoDto n) {
        nodoEJB.deshabilitar(n.getId());
        mensaje("Nodo " + n.getIdentificador() + " deshabilitado.");
        cargar();
    }

    public void cambiarPuntoAcceso(NodoPerifericoDto n) {
        nodoEJB.cambiarPuntoAcceso(n.getId(), n.getPuntoAcceso());
        mensaje("Punto de acceso de " + n.getIdentificador() + " actualizado. El nodo queda deshabilitado hasta que se vuelva a habilitar.");
        cargar();
    }

    public void darDeBaja(NodoPerifericoDto n) {
        nodoEJB.darDeBaja(n.getId());
        mensaje("Nodo " + n.getIdentificador() + " dado de baja.");
        cargar();
    }

    public boolean habilitado(NodoPerifericoDto n) {
        return n.getEstado() == EstadoNodo.HABILITADO;
    }

    public TipoNodo[] getTipos() {
        return TipoNodo.values();
    }

    public List<NodoPerifericoDto> getNodos() {
        return nodos;
    }

    public NodoPerifericoDto getNodo() {
        return nodo;
    }

    public void setNodo(NodoPerifericoDto nodo) {
        this.nodo = nodo;
    }

    private static void mensaje(String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, texto, null));
    }
}
