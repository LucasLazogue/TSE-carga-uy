package tse.cargauy.negocio.sesion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import tse.cargauy.data.refreshtoken.RefreshTokenDAO;
import tse.cargauy.data.usuario.UsuarioDAO;
import tse.cargauy.dtos.RefreshTokenDto;
import tse.cargauy.dtos.TokenDto;
import tse.cargauy.dtos.TokensMovilDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.entities.EstadoRefreshToken;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.usuario.UsuarioEJB;

class SesionMovilEJBTest {

    private static final Long CHOFER = 5001L;
    private static final String URL = "http://192.168.1.50:8080/carga-uy/api/auth/token";

    private final Map<String, RefreshTokenDto> guardados = new HashMap<>();
    private final List<String> usosRegistrados = new ArrayList<>();
    private Instant limiteBorrado;

    private final SesionMovilEJB ejb = crearEjb();

    private SesionMovilEJB crearEjb() {
        // la prueba DPoP falsa "prueba-de-<celular>" vale como la de ese celular; la criptografia esta en DpopTest
        SesionMovilEJB ejb = new SesionMovilEJB() {
            @Override
            String huellaDpop(String dpop, String metodo, String url) {
                if (dpop == null || !dpop.startsWith("prueba-de-")) {
                    throw new CargaUYException(CodigoError.AUTH_DPOP_INVALIDO);
                }
                return dpop.substring("prueba-de-".length());
            }
        };
        ejb.usuarioEJB = new UsuarioEJB() {
            @Override
            public UsuarioDto validarCodigoMobile(String codigo, String codeVerifier) {
                if (!"code-valido".equals(codigo)) {
                    throw new CargaUYException(CodigoError.AUTH_SOLICITUD_INVALIDA);
                }
                return chofer();
            }

            @Override
            public TokenDto crearTokenMovil(UsuarioDto usuario) {
                return new TokenDto("access-de-" + usuario.getId(), 900);
            }
        };
        ejb.usuarioDAO = new UsuarioDAO() {
            @Override
            public UsuarioDto getById(Long id) {
                return CHOFER.equals(id) ? chofer() : null;
            }
        };
        ejb.refreshTokenDAO = new RefreshTokenDAO() {
            @Override
            public void crear(RefreshTokenDto refreshToken) {
                guardados.put(refreshToken.getHash(), refreshToken);
            }

            @Override
            public void revocarActivos(Long idUsuario, String dispositivo) {
                guardados.values().stream()
                        .filter(r -> r.getIdUsuario().equals(idUsuario) && r.getDispositivo().equals(dispositivo))
                        .forEach(r -> r.setEstado(EstadoRefreshToken.REVOCADO));
            }

            @Override
            public RefreshTokenDto getByHash(String hash) {
                return guardados.get(hash);
            }

            @Override
            public void registrarUso(String hash, Instant ahora) {
                usosRegistrados.add(hash);
                guardados.get(hash).setUltimoUso(ahora);
            }

            @Override
            public void revocar(String hash) {
                if (guardados.containsKey(hash)) {
                    guardados.get(hash).setEstado(EstadoRefreshToken.REVOCADO);
                }
            }

            @Override
            public int borrarViejos(Instant limite) {
                limiteBorrado = limite;
                return 0;
            }
        };
        return ejb;
    }

    private static UsuarioDto chofer() {
        UsuarioDto usuario = new UsuarioDto();
        usuario.setId(CHOFER);
        usuario.setRoles(List.of("CHOFER"));
        return usuario;
    }

    private static void assertCodigo(CodigoError esperado, Executable accion) {
        assertEquals(esperado, assertThrows(CargaUYException.class, accion).getCodigo());
    }

    private String ingresar(String celular) {
        return ejb.iniciar("code-valido", "el-verifier", "prueba-de-" + celular, "POST", URL).getRefreshToken();
    }

    private TokensMovilDto renovar(String refreshToken, String celular) {
        return ejb.renovar(refreshToken, "prueba-de-" + celular, "POST", URL);
    }

    private RefreshTokenDto guardado() {
        return guardados.values().iterator().next();
    }

    @Test
    void elLoginDevuelveAccessYRefreshTokenYGuardaSoloElHash() {
        TokensMovilDto tokens = ejb.iniciar("code-valido", "el-verifier", "prueba-de-celular-1", "POST", URL);

        assertEquals("access-de-5001", tokens.getAccessToken());
        assertNotNull(tokens.getRefreshToken());
        assertEquals(Duration.ofDays(7).toSeconds(), tokens.getSegundosRefresh());
        assertNotEquals(tokens.getRefreshToken(), guardado().getHash());
        assertEquals(EstadoRefreshToken.ACTIVO, guardado().getEstado());
        assertEquals("celular-1", guardado().getDispositivo());
    }

    @Test
    void unCodigoInvalidoNoCreaSesion() {
        assertCodigo(CodigoError.AUTH_SOLICITUD_INVALIDA, () -> ejb.iniciar("otro", "el-verifier", "prueba-de-celular-1", "POST", URL));
        assertTrue(guardados.isEmpty());
    }

    @Test
    void renuevaDesdeElMismoDispositivoSinEntregarOtroRefresh() {
        String refresh = ingresar("celular-1");

        TokensMovilDto renovado = renovar(refresh, "celular-1");

        assertEquals("access-de-5001", renovado.getAccessToken());
        assertNull(renovado.getRefreshToken());
    }

    @Test
    void rechazaRenovarConLaClaveDeOtroCelular() {
        String refresh = ingresar("celular-1");

        assertCodigo(CodigoError.AUTH_REFRESH_INVALIDO, () -> renovar(refresh, "celular-2"));
    }

    @Test
    void rechazaRenovarConUnRefreshRevocado() {
        String refresh = ingresar("celular-1");

        ejb.revocar(refresh);

        assertCodigo(CodigoError.AUTH_REFRESH_INVALIDO, () -> renovar(refresh, "celular-1"));
    }

    @Test
    void unLoginNuevoEnElMismoCelularRevocaElAnterior() {
        String anterior = ingresar("celular-1");
        String nuevo = ingresar("celular-1");

        assertCodigo(CodigoError.AUTH_REFRESH_INVALIDO, () -> renovar(anterior, "celular-1"));
        assertNotNull(renovar(nuevo, "celular-1").getAccessToken());
    }

    @Test
    void rechazaUnRefreshSinUsoHaceMasDeUnaSemana() {
        String refresh = ingresar("celular-1");
        guardado().setUltimoUso(Instant.now().minus(Duration.ofDays(8)));

        assertCodigo(CodigoError.AUTH_REFRESH_INVALIDO, () -> renovar(refresh, "celular-1"));
    }

    @Test
    void elUltimoUsoSeEscribeComoMuchoUnaVezPorDia() {
        String refresh = ingresar("celular-1");

        renovar(refresh, "celular-1");
        assertTrue(usosRegistrados.isEmpty());

        guardado().setUltimoUso(Instant.now().minus(Duration.ofDays(2)));
        renovar(refresh, "celular-1");
        assertEquals(1, usosRegistrados.size());
    }

    @Test
    void sinPruebaDpopNoHaySesion() {
        assertCodigo(CodigoError.AUTH_DPOP_INVALIDO, () -> ejb.iniciar("code-valido", "el-verifier", null, "POST", URL));
        assertCodigo(CodigoError.AUTH_DPOP_INVALIDO, () -> ejb.renovar("x", null, "POST", URL));
        assertTrue(guardados.isEmpty());
    }

    @Test
    void revocarUnTokenQueNoExisteNoFalla() {
        ejb.revocar("no-existe");
        ejb.revocar(null);
    }

    @Test
    void borraLosQueDejaronDeServirHaceUnMes() {
        ejb.borrarViejos();

        Instant haceUnMes = Instant.now().minus(Duration.ofDays(30));
        assertTrue(Duration.between(limiteBorrado, haceUnMes).abs().toSeconds() < 5);
    }
}
