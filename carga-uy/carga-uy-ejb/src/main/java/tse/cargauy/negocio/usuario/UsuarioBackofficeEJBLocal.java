package tse.cargauy.negocio.usuario;

import jakarta.ejb.Local;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.UsuarioDto;

@Local
public interface UsuarioBackofficeEJBLocal {
    PaginaDto<UsuarioDto> getUsuarios(Paginacion paginacion);
    void addUsuario(UsuarioDto usuario, String clave);
    void updateUsuario(UsuarioDto usuario, String clave);
    void deleteUsuario(String usuario);
}
