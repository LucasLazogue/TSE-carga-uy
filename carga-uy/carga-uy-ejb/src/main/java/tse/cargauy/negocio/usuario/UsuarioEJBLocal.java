package tse.cargauy.negocio.usuario;

import java.net.URI;

import jakarta.ejb.Local;
import tse.cargauy.dtos.UsuarioDto;

@Local
public interface UsuarioEJBLocal {
    URI getGubUyLoginUrl(String state, String nonce);
    UsuarioDto loginGubUy(String code, String nonce);
}
