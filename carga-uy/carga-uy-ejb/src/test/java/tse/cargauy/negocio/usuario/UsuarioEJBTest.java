package tse.cargauy.negocio.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import jakarta.ejb.SessionContext;
import tse.cargauy.adaptadores.gubuy.GubUyClient;
import tse.cargauy.adaptadores.gubuy.IdentidadGubUy;
import tse.cargauy.adaptadores.pdi.PdiClient;
import tse.cargauy.adaptadores.pdi.PersonaPdi;
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
        conPdi(Optional.empty());
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

    // el login con el DAO, gub.uy y la PDI simulados: los ciudadanos quedan en un mapa por cedula
    private final Map<String, UsuarioDto> ciudadanos = new HashMap<>();
    private final List<String> consultasPdi = new ArrayList<>();
    private UsuarioDto funcionario;

    private void conPdi(Optional<PersonaPdi> respuesta) {
        ejb.pdi = new PdiClient() {
            @Override
            public Optional<PersonaPdi> obtenerPersona(String cedula) {
                consultasPdi.add(cedula);
                return respuesta;
            }
        };
    }

    private void prepararLogin(Optional<PersonaPdi> respuestaPdi) {
        conPdi(respuestaPdi);
        ejb.gubUy = new GubUyClient() {
            @Override
            public IdentidadGubUy exchangeCode(String code, String nonce) {
                return new IdentidadGubUy(code, code + "@correo.uy");
            }
        };
        ejb.usuarioDAO = new UsuarioDAO() {
            @Override
            public UsuarioDto getFuncionarioByCedula(String cedula) {
                return funcionario;
            }

            @Override
            public UsuarioDto getCiudadanoByCedula(String cedula) {
                return ciudadanos.get(cedula);
            }

            @Override
            public UsuarioDto addCiudadano(String cedula, String correo) {
                UsuarioDto nuevo = ciudadano(cedula, null, null);
                ciudadanos.put(cedula, nuevo);
                return nuevo;
            }

            @Override
            public void updateCorreo(Long idCiudadano, String correo) {
                // el correo no importa en estas pruebas
            }

            @Override
            public void updateDatosPdi(Long idCiudadano, String nombres, String apellidos, String fechaNacimiento) {
                UsuarioDto ciudadano = ciudadanos.values().stream()
                        .filter(c -> c.getId().equals(idCiudadano))
                        .findFirst()
                        .orElseThrow();
                ciudadano.setNombres(nombres);
                ciudadano.setApellidos(apellidos);
                ciudadano.setFechaNacimiento(fechaNacimiento);
            }
        };
    }

    private static UsuarioDto ciudadano(String cedula, String nombres, String fechaNacimiento) {
        UsuarioDto ciudadano = new UsuarioDto();
        ciudadano.setId(Long.valueOf(cedula));
        ciudadano.setCedula(cedula);
        ciudadano.setNombres(nombres);
        ciudadano.setFechaNacimiento(fechaNacimiento);
        ciudadano.setRoles(List.of("CIUDADANO"));
        return ciudadano;
    }

    private static Optional<PersonaPdi> persona(String fechaNacimiento) {
        return Optional.of(new PersonaPdi("Ana María", "Pereira Gómez", fechaNacimiento));
    }

    private static String haceAnios(int anios) {
        return LocalDate.now().minusYears(anios).toString();
    }

    @Test
    void elPrimerIngresoCompletaElPerfilConLosDatosDeLaPdi() {
        prepararLogin(persona("1975-11-03"));

        UsuarioDto usuario = ejb.loginGubUy("22222222", "nonce");

        assertEquals(List.of("22222222"), consultasPdi);
        assertEquals("Ana María", usuario.getNombres());
        assertEquals("Pereira Gómez", usuario.getApellidos());
        assertEquals("1975-11-03", usuario.getFechaNacimiento());
        assertEquals("Ana María", ciudadanos.get("22222222").getNombres());
    }

    @Test
    void conLaPdiCaidaEntraSinDatosYSeCompletanEnElProximoIngreso() {
        prepararLogin(Optional.empty());

        UsuarioDto sinDatos = ejb.loginGubUy("22222222", "nonce");

        assertEquals("22222222", sinDatos.getCedula());
        assertNull(sinDatos.getNombres());

        prepararLogin(persona("1975-11-03"));

        assertEquals("Ana María", ejb.loginGubUy("22222222", "nonce").getNombres());
    }

    @Test
    void unPerfilCompletoNoConsultaLaPdi() {
        ciudadanos.put("22222222", ciudadano("22222222", "Ana María", "1975-11-03"));
        prepararLogin(persona("1975-11-03"));

        assertEquals("Ana María", ejb.loginGubUy("22222222", "nonce").getNombres());
        assertTrue(consultasPdi.isEmpty());
    }

    @Test
    void unPerfilCompletoSinFechaEntraSinConsultar() {
        ciudadanos.put("22222222", ciudadano("22222222", "Ana María", null));
        prepararLogin(persona(haceAnios(16)));

        assertEquals("22222222", ejb.loginGubUy("22222222", "nonce").getCedula());
        assertTrue(consultasPdi.isEmpty());
    }

    @Test
    void unFuncionarioNoConsultaLaPdi() {
        funcionario = new UsuarioDto();
        funcionario.setCedula("11111111");
        prepararLogin(persona(haceAnios(16)));

        assertEquals(funcionario, ejb.loginGubUy("11111111", "nonce"));
        assertTrue(consultasPdi.isEmpty());
    }

    @Test
    void unMenorNuevoSeRechazaSinCrearElPerfil() {
        prepararLogin(persona(haceAnios(16)));

        assertCodigo(CodigoError.USUARIO_MENOR_DE_EDAD, () -> ejb.loginGubUy("33333333", "nonce"));
        assertTrue(ciudadanos.isEmpty());
    }

    @Test
    void unMenorConPerfilPendienteGuardaLaFechaYSeRechaza() {
        ciudadanos.put("33333333", ciudadano("33333333", null, null));
        String fecha = haceAnios(16);
        prepararLogin(persona(fecha));

        assertCodigo(CodigoError.USUARIO_MENOR_DE_EDAD, () -> ejb.loginGubUy("33333333", "nonce"));
        assertEquals(fecha, ciudadanos.get("33333333").getFechaNacimiento());
    }

    @Test
    void unMenorConDatosGuardadosSeRechazaSinConsultarLaPdi() {
        ciudadanos.put("33333333", ciudadano("33333333", "Lucia", haceAnios(16)));
        prepararLogin(Optional.empty());

        assertCodigo(CodigoError.USUARIO_MENOR_DE_EDAD, () -> ejb.loginGubUy("33333333", "nonce"));
        assertTrue(consultasPdi.isEmpty());
    }

    @Test
    void elRechazadoEntraCuandoCumpleDieciocho() {
        ciudadanos.put("33333333", ciudadano("33333333", "Lucia", haceAnios(18)));
        prepararLogin(Optional.empty());

        assertEquals("33333333", ejb.loginGubUy("33333333", "nonce").getCedula());
    }

    @Test
    void elTokenMovilDuraQuinceMinutosYSoloLlevaElRolDeChofer() {
        UsuarioDto usuario = chofer();
        usuario.setRoles(List.of("CHOFER", "RESPONSABLE"));

        TokenDto token = ejb.crearTokenMovil(usuario);

        assertEquals(List.of("CHOFER"), ejb.validarToken(token.getToken()).getRoles());
        assertEquals(15 * 60, token.getSegundosVigencia());
    }

    @Test
    void rechazaUsuarioActualQueYaNoExiste() {
        ejb.contexto = (SessionContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { SessionContext.class }, (proxy, metodo, args) ->
                        "getCallerPrincipal".equals(metodo.getName()) ? (Principal) () -> "5" : null);
        ejb.usuarioDAO = new UsuarioDAO() {
            @Override
            public UsuarioDto getById(Long id) {
                return null;
            }
        };

        assertCodigo(CodigoError.AUTH_SESION_INVALIDA, ejb::getUsuarioActual);
    }
}
