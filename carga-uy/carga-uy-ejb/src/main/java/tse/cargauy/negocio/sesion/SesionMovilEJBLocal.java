package tse.cargauy.negocio.sesion;

import jakarta.ejb.Local;
import tse.cargauy.dtos.TokensMovilDto;

// solo Local, como UsuarioEJB: no lo usa el cliente de consola
@Local
public interface SesionMovilEJBLocal {
    TokensMovilDto iniciar(String codigo, String codeVerifier, String dpop, String metodo, String url);
    TokensMovilDto renovar(String refreshToken, String dpop, String metodo, String url);
    void revocar(String refreshToken);
}
