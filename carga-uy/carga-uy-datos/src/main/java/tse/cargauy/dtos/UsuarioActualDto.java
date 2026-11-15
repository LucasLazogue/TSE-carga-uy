package tse.cargauy.dtos;

import java.util.List;

public class UsuarioActualDto extends UsuarioDto {

    private static final long serialVersionUID = 1L;

    private List<EmpresaDto> empresas;

    public UsuarioActualDto() {
    }

    public UsuarioActualDto(UsuarioDto usuario, List<EmpresaDto> empresas) {
        setId(usuario.getId());
        setUsuario(usuario.getUsuario());
        setCedula(usuario.getCedula());
        setCorreo(usuario.getCorreo());
        setNombres(usuario.getNombres());
        setApellidos(usuario.getApellidos());
        setFechaNacimiento(usuario.getFechaNacimiento());
        setRoles(usuario.getRoles());
        this.empresas = empresas;
    }

    public List<EmpresaDto> getEmpresas() {
        return empresas;
    }

    public void setEmpresas(List<EmpresaDto> empresas) {
        this.empresas = empresas;
    }
}
