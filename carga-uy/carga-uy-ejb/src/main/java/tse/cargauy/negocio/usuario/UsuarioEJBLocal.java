package tse.cargauy.negocio.usuario;

import java.net.URI;

import jakarta.ejb.Local;
import tse.cargauy.dtos.TokenDto;
import tse.cargauy.dtos.UsuarioDto;

@Local
public interface UsuarioEJBLocal {
    URI getGubUyLoginUrl(String state, String nonce, String cedulaMock); // TODO mock: sacar cedulaMock
    UsuarioDto loginGubUy(String code, String nonce);
    TokenDto crearToken(UsuarioDto usuario);
    UsuarioDto validarToken(String token);
    String crearCodigoMobile(UsuarioDto usuario, String codeChallenge);
    TokenDto canjearCodigoMobile(String codigo, String codeVerifier);
}
