package tse.cargauy.dtos;

import java.io.Serializable;
import java.util.List;

public class UsuarioActualDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String cedula;
    private String correo;
    private List<String> roles;
    private List<EmpresaDto> empresas;

    public UsuarioActualDto() {
    }

    public UsuarioActualDto(UsuarioDto usuario, List<EmpresaDto> empresas) {
        this.id = usuario.getId();
        this.cedula = usuario.getCedula();
        this.correo = usuario.getCorreo();
        this.roles = usuario.getRoles();
        this.empresas = empresas;
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

    public List<EmpresaDto> getEmpresas() {
        return empresas;
    }

    public void setEmpresas(List<EmpresaDto> empresas) {
        this.empresas = empresas;
    }
}
