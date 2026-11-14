package tse.cargauy.dtos;

import java.io.Serializable;
import java.util.List;

public class UsuarioActualDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String cedula;
    private String correo;
    private String nombres;
    private String apellidos;
    private String fechaNacimiento;
    private List<String> roles;
    private List<EmpresaDto> empresas;

    public UsuarioActualDto() {
    }

    public UsuarioActualDto(UsuarioDto usuario, List<EmpresaDto> empresas) {
        this.id = usuario.getId();
        this.cedula = usuario.getCedula();
        this.correo = usuario.getCorreo();
        this.nombres = usuario.getNombres();
        this.apellidos = usuario.getApellidos();
        this.fechaNacimiento = usuario.getFechaNacimiento();
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

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
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
