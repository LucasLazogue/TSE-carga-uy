package tse.cargauy.web.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.omnifaces.cdi.ViewScoped;

import jakarta.ejb.EJB;
import jakarta.enterprise.inject.Model;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.entities.Rol;
import tse.cargauy.negocio.usuario.FuncionarioEJBLocal;
import tse.cargauy.negocio.usuario.UsuarioBackofficeEJBLocal;

// @Model es solo para que WildFly descubra el bean: dentro del EAR no reconoce como alcance la anotacion de
// OmniFaces, que viene en WEB-INF/lib. vale el alcance explicito (@ViewScoped), no el de pedido de @Model
@Model
@ViewScoped(saveInViewState = true)
public class UsuarioView implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private UsuarioBackofficeEJBLocal usuarioBackofficeEJB;

    @EJB
    private FuncionarioEJBLocal funcionarioEJB;

    private final ModeloPaginado<UsuarioDto> usuarios =
            new ModeloPaginado<>(paginacion -> usuarioBackofficeEJB.getUsuarios(paginacion));
    private final ModeloPaginado<UsuarioDto> funcionarios =
            new ModeloPaginado<>(paginacion -> funcionarioEJB.getFuncionarios(paginacion));
    private UsuarioDto usuario = new UsuarioDto(null, new ArrayList<>());
    private String clave;
    private boolean edicion;
    private String cedula;

    public void nuevo() {
        usuario = new UsuarioDto(null, new ArrayList<>());
        clave = null;
        edicion = false;
    }

    public void editar(UsuarioDto u) {
        usuario = new UsuarioDto(u.getUsuario(), new ArrayList<>(u.getRoles()));
        clave = null;
        edicion = true;
    }

    public void guardar() {
        if (edicion) {
            usuarioBackofficeEJB.updateUsuario(usuario, clave);
            mensaje("Usuario " + usuario.getUsuario() + " actualizado.");
        } else {
            usuarioBackofficeEJB.addUsuario(usuario, clave);
            mensaje("Usuario " + usuario.getUsuario() + " registrado.");
        }
        // el bean viaja (cifrado) en la pagina: la clave no tiene que quedar en el
        clave = null;
    }

    public void deleteUsuario(UsuarioDto u) {
        usuarioBackofficeEJB.deleteUsuario(u.getUsuario());
        mensaje("Usuario " + u.getUsuario() + " eliminado.");
    }

    public void nuevoFuncionario() {
        cedula = null;
    }

    public void addFuncionario() {
        funcionarioEJB.addFuncionario(cedula);
        mensaje("Funcionario " + cedula + " registrado.");
        cedula = null;
    }

    public void deleteFuncionario(UsuarioDto f) {
        funcionarioEJB.deleteFuncionario(f.getCedula());
        mensaje("Funcionario " + f.getCedula() + " eliminado.");
    }

    public List<String> getRolesBackoffice() {
        return Rol.BACKOFFICE;
    }

    public ModeloPaginado<UsuarioDto> getUsuarios() {
        return usuarios;
    }

    public ModeloPaginado<UsuarioDto> getFuncionarios() {
        return funcionarios;
    }

    public UsuarioDto getUsuario() {
        return usuario;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public boolean isEdicion() {
        return edicion;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    private static void mensaje(String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, texto, null));
    }
}
