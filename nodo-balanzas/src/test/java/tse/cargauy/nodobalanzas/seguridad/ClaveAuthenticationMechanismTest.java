package tse.cargauy.nodobalanzas.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.HttpServletRequest;
import tse.cargauy.nodobalanzas.data.peaje.PeajeDAO;

// quien llama al nodo, segun la clave que manda: el central, el administrador o una balanza (cada una con la suya)
class ClaveAuthenticationMechanismTest {

    private final ClaveAuthenticationMechanism mecanismo = new ClaveAuthenticationMechanism();
    private final List<String> logins = new ArrayList<>();

    private final HttpMessageContext contexto = (HttpMessageContext) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] { HttpMessageContext.class }, (proxy, metodo, args) -> switch (metodo.getName()) {
                case "notifyContainerAboutLogin" -> {
                    logins.add(args[0] + " " + args[1]);
                    yield AuthenticationStatus.SUCCESS;
                }
                case "doNothing" -> AuthenticationStatus.NOT_DONE;
                default -> null;
            });

    @BeforeEach
    void preparar() {
        mecanismo.claveCentral = "clave-central";
        mecanismo.claveAdmin = "clave-admin";
        mecanismo.peajeDAO = new PeajeDAO() {
            @Override
            public String getCodigoBalanzaByClave(String claveHash) {
                return Sha256.base64Url("clave-balanza").equals(claveHash) ? "balanza-r5-km30" : null;
            }
        };
    }

    private static HttpServletRequest pedido(String authorization) {
        return (HttpServletRequest) Proxy.newProxyInstance(ClaveAuthenticationMechanismTest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class },
                (proxy, metodo, args) -> metodo.getName().equals("getHeader") && "Authorization".equals(args[0]) ? authorization : null);
    }

    private AuthenticationStatus validar(String authorization) {
        return mecanismo.validateRequest(pedido(authorization), null, contexto);
    }

    @Test
    void elCentralYElAdministrador() {
        assertEquals(AuthenticationStatus.SUCCESS, validar("Bearer clave-central"));
        assertEquals(AuthenticationStatus.SUCCESS, validar("Bearer clave-admin"));

        assertEquals(List.of("CENTRAL " + Set.of(Rol.CENTRAL), "ADMINISTRADOR " + Set.of(Rol.ADMINISTRADOR)), logins);
    }

    // la balanza se identifica por el hash de su clave; queda logueada con su codigo
    @Test
    void unaBalanza() {
        assertEquals(AuthenticationStatus.SUCCESS, validar("Bearer clave-balanza"));

        assertEquals(List.of("balanza-r5-km30 " + Set.of(Rol.BALANZA)), logins);
    }

    @Test
    void sinClaveOConUnaQueNoEsDeNadie() {
        assertEquals(AuthenticationStatus.NOT_DONE, validar(null));
        assertEquals(AuthenticationStatus.NOT_DONE, validar("Basic dXNlcjpjbGF2ZQ=="));
        assertEquals(AuthenticationStatus.NOT_DONE, validar("Bearer clave-centra"));
        assertEquals(AuthenticationStatus.NOT_DONE, validar("Bearer otra"));

        assertTrue(logins.isEmpty());
    }

    @Test
    void elHashEsSha256EnBase64Url() {
        assertEquals("47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU", Sha256.base64Url(""));
    }
}
