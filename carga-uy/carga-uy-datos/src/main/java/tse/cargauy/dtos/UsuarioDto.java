package tse.cargauy.dtos;

import java.io.Serializable;
import java.util.List;

public class UsuarioDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String cedula;
    private String correo;
    private List<String> roles;

    public UsuarioDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        return "Cedula: " + cedula + ", Correo: " + correo + ", Roles: " + roles;
    }
}
