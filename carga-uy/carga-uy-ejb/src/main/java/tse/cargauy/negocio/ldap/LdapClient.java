package tse.cargauy.negocio.ldap;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStoreHandler;
import jakarta.security.enterprise.identitystore.LdapIdentityStoreDefinition;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@Named
@ApplicationScoped
@LdapIdentityStoreDefinition(
        url = "${ldapClient.url}",
        bindDn = "cn=Directory Manager",
        bindDnPassword = "${ldapClient.claveDirectoryManager}",
        callerBaseDn = "ou=usuarios,dc=carga,dc=uy",
        callerNameAttribute = "uid",
        groupSearchBase = "ou=grupos,dc=carga,dc=uy",
        groupMemberAttribute = "member",
        groupNameAttribute = "cn")
public class LdapClient {

    @Inject
    IdentityStoreHandler identityStoreHandler;

    @Timeout(3000)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 30000, skipOn = CargaUYException.class)
    @Fallback(fallbackMethod = "noDisponible", skipOn = CargaUYException.class)
    public IdentidadLdap autenticar(String usuario, String clave) {
        CredentialValidationResult resultado = identityStoreHandler.validate(
                new UsernamePasswordCredential(usuario, clave));
        if (resultado.getStatus() != CredentialValidationResult.Status.VALID) {
            throw new CargaUYException(CodigoError.AUTH_CREDENCIALES_INVALIDAS);
        }
        return new IdentidadLdap(resultado.getCallerPrincipal().getName(), resultado.getCallerGroups());
    }

    IdentidadLdap noDisponible(String usuario, String clave) {
        throw new CargaUYException(CodigoError.AUTH_DIRECTORIO_NO_DISPONIBLE);
    }

    public String getUrl() {
        return System.getenv().getOrDefault("LDAP_URL", "ldap://ldap:3389");
    }

    public String getClaveDirectoryManager() {
        return System.getenv("LDAP_DM_PASSWORD");
    }
}
