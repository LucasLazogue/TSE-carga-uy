package tse.cargauy.negocio.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import tse.cargauy.data.usuario.UsuarioDAO;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.ldap.LdapClient;

class UsuarioBackofficeEJBTest {

    private final List<String> llamadas = new ArrayList<>();
    private final UsuarioBackofficeEJB ejb = new UsuarioBackofficeEJB() {
        @Override
        String getUsuarioSesion() {
            return "admin";
        }
    };

    @BeforeEach
    void iniciar() {
        ejb.ldap = new LdapClient() {
            @Override
            public void addUsuario(UsuarioDto usuario, String clave) {
                llamadas.add("ldap.add " + usuario.getUsuario());
            }

            @Override
            public void updateUsuario(UsuarioDto usuario) {
                llamadas.add("ldap.update " + usuario.getUsuario() + " " + usuario.getRoles());
            }

            @Override
            public void updateClave(String usuario, String clave) {
                llamadas.add("ldap.clave " + usuario);
            }

            @Override
            public void deleteUsuario(String usuario) {
                llamadas.add("ldap.delete " + usuario);
            }
        };
        ejb.usuarioDAO = new UsuarioDAO() {
            @Override
            public void deleteUsuarioBackoffice(String usuario) {
                llamadas.add("dao.delete " + usuario);
            }
        };
    }

    private static void assertCodigo(CodigoError esperado, Executable accion) {
        assertEquals(esperado, assertThrows(CargaUYException.class, accion).getCodigo());
    }

    @Test
    void altaConRolYClave() {
        ejb.addUsuario(new UsuarioDto("ana", List.of("AUTORIDAD")), "clave");

        assertEquals(List.of("ldap.add ana"), llamadas);
    }

    @Test
    void altaSinUsuarioClaveORolValido() {
        assertCodigo(CodigoError.USUARIO_BACKOFFICE_USUARIO_REQUERIDO,
                () -> ejb.addUsuario(new UsuarioDto(" ", List.of("AUTORIDAD")), "clave"));
        assertCodigo(CodigoError.USUARIO_BACKOFFICE_CLAVE_REQUERIDA,
                () -> ejb.addUsuario(new UsuarioDto("ana", List.of("AUTORIDAD")), ""));
        assertCodigo(CodigoError.USUARIO_BACKOFFICE_ROL_REQUERIDO,
                () -> ejb.addUsuario(new UsuarioDto("ana", List.of()), "clave"));
        assertCodigo(CodigoError.USUARIO_BACKOFFICE_ROL_REQUERIDO,
                () -> ejb.addUsuario(new UsuarioDto("ana", List.of("FUNCIONARIO")), "clave"));
        assertEquals(List.of(), llamadas);
    }

    @Test
    void modificacionCambiaClaveSoloSiViene() {
        ejb.updateUsuario(new UsuarioDto("ana", List.of("ADMINISTRADOR")), null);
        ejb.updateUsuario(new UsuarioDto("ana", List.of("AUTORIDAD")), "nueva");

        assertEquals(List.of("ldap.update ana [ADMINISTRADOR]", "ldap.update ana [AUTORIDAD]", "ldap.clave ana"),
                llamadas);
    }

    @Test
    void noSePuedeQuitarSuPropioRolDeAdministrador() {
        assertCodigo(CodigoError.USUARIO_BACKOFFICE_PROPIO,
                () -> ejb.updateUsuario(new UsuarioDto("admin", List.of("AUTORIDAD")), null));
        ejb.updateUsuario(new UsuarioDto("admin", List.of("ADMINISTRADOR", "AUTORIDAD")), null);

        assertEquals(List.of("ldap.update admin [ADMINISTRADOR, AUTORIDAD]"), llamadas);
    }

    @Test
    void bajaBorraLaBaseAntesQueElDirectorio() {
        ejb.deleteUsuario("ana");

        assertEquals(List.of("dao.delete ana", "ldap.delete ana"), llamadas);
    }

    @Test
    void noSePuedeDarDeBajaASiMismo() {
        assertCodigo(CodigoError.USUARIO_BACKOFFICE_PROPIO, () -> ejb.deleteUsuario("admin"));
        assertEquals(List.of(), llamadas);
    }
}
