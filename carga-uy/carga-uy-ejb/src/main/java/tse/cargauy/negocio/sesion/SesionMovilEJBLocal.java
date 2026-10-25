package tse.cargauy.negocio.sesion;

import jakarta.ejb.Local;
import tse.cargauy.dtos.TokensMovilDto;

// solo Local, como UsuarioEJB: lo usa /auth, no el cliente de consola
@Local
public interface SesionMovilEJBLocal {
    TokensMovilDto iniciar(String codigo, String codeVerifier, String dispositivo);
    TokensMovilDto renovar(String refreshToken, String dispositivo);
    void revocar(String refreshToken);
}
