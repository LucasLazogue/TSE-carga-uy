package tse.cargauy.nodobalanzas.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ApplicationScoped
public class ClaveAuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final String BEARER = "Bearer ";

    @Inject
    @ConfigProperty(name = "nodo.clave.central")
    String claveCentral;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response,
            HttpMessageContext contexto) {
        String clave = clave(request);
        if (clave != null && coincide(clave, claveCentral)) {
            return contexto.notifyContainerAboutLogin(Rol.CENTRAL, Set.of(Rol.CENTRAL));
        }
        return contexto.doNothing();
    }

    private static String clave(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER)) {
            return authorization.substring(BEARER.length());
        }
        return null;
    }

    // MessageDigest.isEqual tarda lo mismo sin importar en que letra difieren las claves.
    // con un equals comun, una clave que arranca bien tarda un poquito mas en fallar que una que arranca mal,
    // y midiendo esos tiempos alguien podria adivinar la clave letra por letra
    private static boolean coincide(String recibida, String esperada) {
        return MessageDigest.isEqual(recibida.getBytes(StandardCharsets.UTF_8), esperada.getBytes(StandardCharsets.UTF_8));
    }
}
