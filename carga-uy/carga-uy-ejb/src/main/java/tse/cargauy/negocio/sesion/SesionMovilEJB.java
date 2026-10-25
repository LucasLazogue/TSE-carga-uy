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
import tse.cargauy.seguridad.Sha256;

// la sesion de la app movil: un access token corto y un refresh token por chofer y celular (RFC 6749 1.5).
// UsuarioEJB dice quien es el usuario y firma el access token; aca se maneja la vida del refresh token.
// el estado esta en la base y no en el nodo: cualquier nodo atiende cualquier pedido
@Stateless
public class SesionMovilEJB implements SesionMovilEJBLocal {

    private static final Duration DURACION_REFRESH = Duration.ofDays(30);
    private static final Duration INACTIVIDAD = Duration.ofDays(7);
    // ultimoUso se escribe a lo sumo una vez por dia: la inactividad se mide en dias, y asi casi todas las
    // renovaciones (una cada 15 minutos por chofer) son solo una lectura
    private static final Duration PRECISION_USO = Duration.ofDays(1);
    // los que ya no sirven se guardan un tiempo para poder revisarlos y despues se borran
    private static final Duration RETENCION = Duration.ofDays(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    @EJB
    UsuarioEJBLocal usuarioEJB;

    @EJB
    UsuarioDAOLocal usuarioDAO;

    @EJB
    RefreshTokenDAOLocal refreshTokenDAO;

    // el login: el code de la vuelta de gub.uy y el verifier de PKCE, canjeados por los dos tokens
    public TokensMovilDto iniciar(String codigo, String codeVerifier, String dispositivo) {
        exigirDispositivo(dispositivo);
        UsuarioDto usuario = usuarioEJB.validarCodigoMobile(codigo, codeVerifier);
        // 32 bytes al azar, como el state del login. se guarda solo el hash: si la base se filtra, no sirven
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant ahora = Instant.now();
        RefreshTokenDto guardado = new RefreshTokenDto(Sha256.base64Url(refreshToken), usuario.getId(), dispositivo,
                EstadoRefreshToken.ACTIVO, ahora, ahora, ahora.plus(DURACION_REFRESH));
        // uno activo por chofer y celular: el login nuevo deja sin efecto al anterior
        refreshTokenDAO.revocarActivos(usuario.getId(), dispositivo);
        refreshTokenDAO.crear(guardado);
        return tokens(usuario, refreshToken, guardado, ahora);
    }

    // RFC 6749 6: un access token nuevo sin volver a pasar por gub.uy. sirve aunque el chofer ya no este logueado
    // en el celular (offline access): asi se envian sus eventos. los roles salen de la base, no del token viejo.
    // sin rotacion: dos renovaciones a la vez (en dos nodos) funcionan las dos
    public TokensMovilDto renovar(String refreshToken, String dispositivo) {
        exigirDispositivo(dispositivo);
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

    // RFC 7009: responde igual exista o no el token, para no revelar cuales son validos
    public void revocar(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenDAO.revocar(Sha256.base64Url(refreshToken));
        }
    }

    // todos los dias a las 4. persistent = false: corre en cada nodo, y no importa porque borrar dos veces da lo mismo
    @Schedule(hour = "4", minute = "0", persistent = false)
    public void borrarViejos() {
        refreshTokenDAO.borrarViejos(Instant.now().minus(RETENCION));
    }

    private TokensMovilDto tokens(UsuarioDto usuario, String refreshToken, RefreshTokenDto guardado, Instant ahora) {
        TokenDto acceso = usuarioEJB.crearTokenMovil(usuario);
        return new TokensMovilDto(acceso.getToken(), acceso.getSegundosVigencia(), refreshToken,
                Duration.between(ahora, venceEn(guardado)).toSeconds());
    }

    private static void exigirDispositivo(String dispositivo) {
        if (dispositivo == null || dispositivo.isBlank()) {
            throw new CargaUYException(CodigoError.AUTH_DISPOSITIVO_REQUERIDO);
        }
    }

    static boolean vigente(RefreshTokenDto token, String dispositivo, Instant ahora) {
        return token.getEstado() == EstadoRefreshToken.ACTIVO && token.getDispositivo().equals(dispositivo)
                && ahora.isBefore(venceEn(token));
    }

    // lo que llegue primero: el vencimiento fijo o una semana sin usarse (RFC 9700 4.14)
    private static Instant venceEn(RefreshTokenDto token) {
        Instant porInactividad = token.getUltimoUso().plus(INACTIVIDAD);
        return porInactividad.isBefore(token.getExpira()) ? porInactividad : token.getExpira();
    }
}
