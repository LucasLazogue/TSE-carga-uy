package tse.cargauy.data.usuario;

import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.entities.Ciudadano;
import tse.cargauy.entities.Funcionario;
import tse.cargauy.entities.Rol;
import tse.cargauy.entities.Usuario;
import tse.cargauy.entities.UsuarioBackoffice;

public class Serializers {
    public static UsuarioDto toDto(Usuario usuario) {
        UsuarioDto dto = new UsuarioDto();
        dto.setId(usuario.getId());
        dto.setRoles(usuario.getRoles().stream().map(Rol::getNombre).sorted().toList());
        if (usuario instanceof Ciudadano ciudadano) {
            dto.setCedula(ciudadano.getCedula());
            dto.setCorreo(ciudadano.getCorreo());
        } else if (usuario instanceof Funcionario funcionario) {
            dto.setCedula(funcionario.getCedula());
        } else if (usuario instanceof UsuarioBackoffice backoffice) {
            dto.setUsuario(backoffice.getUsuario());
        }
        return dto;
    }

}
