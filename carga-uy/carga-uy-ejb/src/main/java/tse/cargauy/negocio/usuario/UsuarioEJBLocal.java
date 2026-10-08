package tse.cargauy.negocio.usuario;

import java.net.URI;

import jakarta.ejb.Local;
import tse.cargauy.dtos.EstadoLoginDto;
import tse.cargauy.dtos.TokenDto;
import tse.cargauy.dtos.UsuarioActualDto;
import tse.cargauy.dtos.UsuarioDto;

@Local
public interface UsuarioEJBLocal {
    boolean isLoginMock(); // TODO mock: borrar
    URI getGubUyLoginUrl(String state, String nonce, String cedulaMock); // TODO mock: sacar cedulaMock
    TokenDto crearEstadoLogin(String state, String nonce, String codeChallenge);
    EstadoLoginDto leerEstadoLogin(String token);
    UsuarioDto loginGubUy(String code, String nonce);
    TokenDto crearToken(UsuarioDto usuario);
    UsuarioDto validarToken(String token);

    UsuarioActualDto getUsuarioActual();
    String crearCodigoMobile(UsuarioDto usuario, String codeChallenge);
    TokenDto canjearCodigoMobile(String codigo, String codeVerifier);
}
