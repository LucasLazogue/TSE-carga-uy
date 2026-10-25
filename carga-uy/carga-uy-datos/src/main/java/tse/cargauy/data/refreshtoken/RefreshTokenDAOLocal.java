package tse.cargauy.data.refreshtoken;

import java.time.Instant;

import jakarta.ejb.Local;
import tse.cargauy.dtos.RefreshTokenDto;

@Local
public interface RefreshTokenDAOLocal {
    void crear(RefreshTokenDto refreshToken);
    void revocarActivos(Long idUsuario, String dispositivo);
    RefreshTokenDto getByHash(String hash);
    void registrarUso(String hash, Instant ahora);
    void revocar(String hash);
    int borrarViejos(Instant limite);
}
