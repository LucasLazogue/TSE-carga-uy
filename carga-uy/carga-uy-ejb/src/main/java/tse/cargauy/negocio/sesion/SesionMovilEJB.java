package tse.cargauy.negocio.sesion;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import tse.cargauy.data.refreshtoken.RefreshTokenDAOLocal;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.dtos.RefreshTokenDto;
import tse.cargauy.dtos.TokenDto;
import tse.cargauy.dtos.TokensMovilDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.entities.EstadoRefreshToken;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.usuario.UsuarioEJBLocal;
import tse.cargauy.seguridad.Dpop;
import tse.cargauy.seguridad.Sha256;

// la vida del refresh token de la app movil. UsuarioEJB verifica la identidad y firma el access token
@Stateless
public class SesionMovilEJB implements SesionMovilEJBLocal {

    private static final Duration DURACION_REFRESH = Duration.ofDays(30);
    private static final Duration INACTIVIDAD = Duration.ofDays(7);
    // asi casi todas las renovaciones, una cada 15 minutos por chofer, son solo una lectura
    private static final Duration PRECISION_USO = Duration.ofDays(1);
    private static final Duration RETENCION = Duration.ofDays(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    @EJB
    UsuarioEJBLocal usuarioEJB;

    @EJB
    UsuarioDAOLocal usuarioDAO;

    @EJB
    RefreshTokenDAOLocal refreshTokenDAO;

    public TokensMovilDto iniciar(String codigo, String codeVerifier, String dpop, String metodo, String url) {
        String dispositivo = huellaDpop(dpop, metodo, url);
        UsuarioDto usuario = usuarioEJB.validarCodigoMobile(codigo, codeVerifier);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant ahora = Instant.now();
        RefreshTokenDto guardado = new RefreshTokenDto(Sha256.base64Url(refreshToken), usuario.getId(), dispositivo,
                EstadoRefreshToken.ACTIVO, ahora, ahora, ahora.plus(DURACION_REFRESH));
        refreshTokenDAO.revocarActivos(usuario.getId(), dispositivo);
        refreshTokenDAO.crear(guardado);
        return tokens(usuario, refreshToken, guardado, ahora);
    }

    // sin rotacion: con DPoP no hace falta (RFC 9700 2.2.2)
    public TokensMovilDto renovar(String refreshToken, String dpop, String metodo, String url) {
        String dispositivo = huellaDpop(dpop, metodo, url);
        String hash = refreshToken == null ? null : Sha256.base64Url(refreshToken);
        RefreshTokenDto guardado = hash == null ? null : refreshTokenDAO.getByHash(hash);
        Instant ahora = Instant.now();
        if (guardado == null || !vigente(guardado, dispositivo, ahora)) {
            throw new CargaUYException(CodigoError.AUTH_REFRESH_INVALIDO);
        }
        UsuarioDto usuario = usuarioDAO.getById(guardado.getIdUsuario());
        if (usuario == null) {
            throw new CargaUYException(CodigoError.AUTH_REFRESH_INVALIDO);
        }
        if (guardado.getUltimoUso().isBefore(ahora.minus(PRECISION_USO))) {
            refreshTokenDAO.registrarUso(hash, ahora);
            guardado.setUltimoUso(ahora);
        }
        return tokens(usuario, null, guardado, ahora);
    }

    // RFC 7009: no revela si el token existia
    public void revocar(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenDAO.revocar(Sha256.base64Url(refreshToken));
        }
    }

    // persistent = false: corre en cada nodo, y el DELETE es idempotente
    @Schedule(hour = "4", minute = "0", persistent = false)
    public void borrarViejos() {
        refreshTokenDAO.borrarViejos(Instant.now().minus(RETENCION));
    }

    private TokensMovilDto tokens(UsuarioDto usuario, String refreshToken, RefreshTokenDto guardado, Instant ahora) {
        TokenDto acceso = usuarioEJB.crearTokenMovil(usuario);
        return new TokensMovilDto(acceso.getToken(), acceso.getSegundosVigencia(), refreshToken,
                Duration.between(ahora, venceEn(guardado)).toSeconds());
    }

    // package-private para reemplazarla en los tests
    String huellaDpop(String dpop, String metodo, String url) {
        String huella = Dpop.verificar(dpop, metodo, url, Instant.now());
        if (huella == null) {
            throw new CargaUYException(CodigoError.AUTH_DPOP_INVALIDO);
        }
        return huella;
    }

    static boolean vigente(RefreshTokenDto token, String dispositivo, Instant ahora) {
        return token.getEstado() == EstadoRefreshToken.ACTIVO && token.getDispositivo().equals(dispositivo)
                && ahora.isBefore(venceEn(token));
    }

    private static Instant venceEn(RefreshTokenDto token) {
        Instant porInactividad = token.getUltimoUso().plus(INACTIVIDAD);
        return porInactividad.isBefore(token.getExpira()) ? porInactividad : token.getExpira();
    }
}
