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
    UsuarioDto loginBackoffice(String usuario, String clave);
    TokenDto crearToken(UsuarioDto usuario);
    TokenDto crearTokenMovil(UsuarioDto usuario);
    UsuarioDto validarToken(String token);

    UsuarioDto getUsuarioSesion();
    UsuarioActualDto getUsuarioActual();
    String crearCodigoMobile(UsuarioDto usuario, String codeChallenge);
    UsuarioDto validarCodigoMobile(String codigo, String codeVerifier);
}
