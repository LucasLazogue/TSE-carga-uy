package tse.cargauy.negocio.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import tse.cargauy.dtos.EstadoLoginDto;
import tse.cargauy.dtos.TokenDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

class UsuarioEJBTest {

    private final UsuarioEJB ejb = conSecreto("secreto-de-prueba-de-al-menos-32-bytes");

    private static UsuarioEJB conSecreto(String secreto) {
        return new UsuarioEJB() {
            @Override
            String leerSecreto() {
                return secreto;
            }
        };
    }

    private static UsuarioDto chofer() {
        UsuarioDto usuario = new UsuarioDto();
        usuario.setId(5001L);
        usuario.setCedula("55555555");
        usuario.setRoles(List.of("CHOFER"));
        return usuario;
    }

    private static void assertCodigo(CodigoError esperado, Executable accion) {
        assertEquals(esperado, assertThrows(CargaUYException.class, accion).getCodigo());
    }

    @Test
    void leeElEstadoDeLoginQueFirma() {
        TokenDto estado = ejb.crearEstadoLogin("el-state", "el-nonce", "el-challenge");
        EstadoLoginDto leido = ejb.leerEstadoLogin(estado.getToken());

        assertEquals("el-state", leido.getState());
        assertEquals("el-nonce", leido.getNonce());
        assertEquals("el-challenge", leido.getCodeChallenge());
        assertEquals(300, estado.getSegundosVigencia());
    }

    @Test
    void estadoDeLoginWebNoTieneChallenge() {
        assertNull(ejb.leerEstadoLogin(ejb.crearEstadoLogin("s", "n", null).getToken()).getCodeChallenge());
    }

    @Test
    void rechazaSinCookieDeLogin() {
        assertCodigo(CodigoError.AUTH_SOLICITUD_INVALIDA, () -> ejb.leerEstadoLogin(null));
    }

    @Test
    void rechazaEstadoDeLoginConOtroSecreto() {
        String estado = conSecreto("otro-secreto-de-prueba-de-al-menos-32-bytes").crearEstadoLogin("s", "n", null).getToken();

        assertCodigo(CodigoError.AUTH_SOLICITUD_INVALIDA, () -> ejb.leerEstadoLogin(estado));
    }

    @Test
    void noConfundeEstadoDeLoginConSesion() {
        String estado = ejb.crearEstadoLogin("s", "n", null).getToken();
        String sesion = ejb.crearToken(chofer()).getToken();

        assertCodigo(CodigoError.AUTH_SESION_INVALIDA, () -> ejb.validarToken(estado));
        assertCodigo(CodigoError.AUTH_SOLICITUD_INVALIDA, () -> ejb.leerEstadoLogin(sesion));
    }

    @Test
    void validaLaSesionQueCrea() {
        UsuarioDto usuario = ejb.validarToken(ejb.crearToken(chofer()).getToken());

        assertEquals(5001L, usuario.getId());
        assertEquals("55555555", usuario.getCedula());
        assertEquals(List.of("CHOFER"), usuario.getRoles());
        assertNull(usuario.getCorreo());
    }

    @Test
    void rechazaSecretoCorto() {
        assertCodigo(CodigoError.AUTH_SECRETO_CORTO, () -> conSecreto("corto").crearToken(chofer()));
    }

    @Test
    void rechazaSinSecreto() {
        assertCodigo(CodigoError.AUTH_SECRETO_NO_CONFIGURADO, () -> conSecreto(null).crearToken(chofer()));
    }
}
