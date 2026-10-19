package tse.cargauy.negocio.ldap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;

import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

class LdapClientTest {

    private InMemoryDirectoryServer servidor;
    private final LdapClient ldap = new LdapClient();

    @BeforeEach
    void iniciar() throws Exception {
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=carga,dc=uy");
        config.setSchema(null);
        servidor = new InMemoryDirectoryServer(config);
        servidor.add("dn: dc=carga,dc=uy", "objectClass: domain", "dc: carga");
        servidor.importFromLDIF(false, "../ldap/directorio.ldif");
        ldap.directorio = servidor;
    }

    @AfterEach
    void detener() {
        servidor.shutDown(true);
    }

    @Test
    void altaModificacionYBaja() throws Exception {
        ldap.addUsuario(new UsuarioDto("ana,perez", List.of("ADMINISTRADOR")), "clave123");
        assertEquals(List.of("ADMINISTRADOR"), ldap.getUsuarios().get(0).getRoles());
        assertEquals("ana,perez", ldap.getUsuarios().get(0).getUsuario());

        ldap.updateUsuario(new UsuarioDto("ana,perez", List.of("AUTORIDAD")));
        assertEquals(List.of("AUTORIDAD"), ldap.getUsuarios().get(0).getRoles());

        ldap.updateClave("ana,perez", "otra");
        assertEquals(1, servidor.search("uid=ana\\,perez,ou=usuarios,dc=carga,dc=uy",
                com.unboundid.ldap.sdk.SearchScope.BASE, "(userPassword=otra)").getEntryCount());

        ldap.deleteUsuario("ana,perez");
        assertEquals(List.of(), ldap.getUsuarios());
        assertEquals(0, servidor.search("ou=grupos,dc=carga,dc=uy",
                com.unboundid.ldap.sdk.SearchScope.ONE, "(member=*)").getEntryCount());
    }

    @Test
    void altaDuplicada() {
        ldap.addUsuario(new UsuarioDto("ana", List.of()), "clave123");
        CargaUYException e = assertThrows(CargaUYException.class,
                () -> ldap.addUsuario(new UsuarioDto("ana", List.of()), "clave123"));
        assertEquals(CodigoError.USUARIO_BACKOFFICE_DUPLICADO, e.getCodigo());
    }

    @Test
    void bajaInexistente() {
        CargaUYException e = assertThrows(CargaUYException.class, () -> ldap.deleteUsuario("nadie"));
        assertEquals(CodigoError.USUARIO_BACKOFFICE_NO_ENCONTRADO, e.getCodigo());
    }
}
