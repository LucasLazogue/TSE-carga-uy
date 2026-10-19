package tse.cargauy.negocio.usuario;

import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.UsuarioDto;

@Local
public interface UsuarioBackofficeEJBLocal {
    List<UsuarioDto> getUsuarios();
    void addUsuario(UsuarioDto usuario, String clave);
    void updateUsuario(UsuarioDto usuario, String clave);
    void deleteUsuario(String usuario);
}
