package tse.cargauy.negocio.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import tse.cargauy.adaptadores.gubuy.GubUyClient;
import tse.cargauy.adaptadores.gubuy.IdentidadGubUy;
import tse.cargauy.data.usuario.UsuarioDAO;
import tse.cargauy.dtos.EstadoLoginDto;
import tse.cargauy.dtos.TokenDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.ldap.IdentidadLdap;
import tse.cargauy.negocio.ldap.LdapClient;

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

    @Test
    void losGruposDelDirectorioSonLosRolesDelBackoffice() {
        assertEquals(List.of("ADMINISTRADOR", "AUTORIDAD"),
                UsuarioEJB.rolesBackoffice(Set.of("autoridad", "administrador")));
    }

    @Test
    void ignoraLosGruposQueNoSonDelBackoffice() {
        assertEquals(List.of("AUTORIDAD"), UsuarioEJB.rolesBackoffice(Set.of("autoridad", "otro")));
    }

    @Test
    void rechazaElBackofficeSinGrupoDelBackoffice() {
        ejb.ldap = new LdapClient() {
            @Override
            public IdentidadLdap autenticar(String usuario, String clave) {
                return new IdentidadLdap(usuario, Set.of("otro"));
            }
        };

        assertCodigo(CodigoError.ACCESO_DENEGADO, () -> ejb.loginBackoffice("admin1", "clave"));
    }

    private Map<Long, String> correosGuardados(UsuarioDto existente) {
        Map<Long, String> correos = new HashMap<>();
        ejb.gubUy = new GubUyClient() {
            @Override
            public IdentidadGubUy exchangeCode(String code, String nonce) {
                return new IdentidadGubUy("12345672", "responsable@correo.uy");
            }
        };
        ejb.usuarioDAO = new UsuarioDAO() {
            @Override
            public UsuarioDto getFuncionarioByCedula(String cedula) {
                return null;
            }

            @Override
            public UsuarioDto getCiudadanoByCedula(String cedula) {
                return existente;
            }

            @Override
            public void updateCorreo(Long idCiudadano, String correo) {
                correos.put(idCiudadano, correo);
            }
        };
        return correos;
    }

    @Test
    void elResponsableDadoDeAltaSinIngresarEntraConSuRolYCompletaElCorreo() {
        UsuarioDto existente = new UsuarioDto();
        existente.setId(7L);
        existente.setRoles(List.of("CIUDADANO", "RESPONSABLE"));
        Map<Long, String> correos = correosGuardados(existente);

        UsuarioDto usuario = ejb.loginGubUy("code", "nonce");

        assertEquals(List.of("CIUDADANO", "RESPONSABLE"), usuario.getRoles());
        assertEquals("responsable@correo.uy", usuario.getCorreo());
        assertEquals(Map.of(7L, "responsable@correo.uy"), correos);
    }

    @Test
    void noPisaElCorreoQueYaTenia() {
        UsuarioDto existente = new UsuarioDto();
        existente.setId(7L);
        existente.setCorreo("anterior@correo.uy");
        Map<Long, String> correos = correosGuardados(existente);

        assertEquals("anterior@correo.uy", ejb.loginGubUy("code", "nonce").getCorreo());
        assertTrue(correos.isEmpty());
    }
}
