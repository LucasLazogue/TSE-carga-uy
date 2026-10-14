package tse.cargauy.web.seguridad;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStoreHandler;
import jakarta.security.enterprise.identitystore.LdapIdentityStoreDefinition;
import tse.cargauy.dtos.TokenDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.usuario.UsuarioEJBLocal;

@Named
@ApplicationScoped
@LdapIdentityStoreDefinition(
        url = "${directorioBackoffice.url}",
        bindDn = "cn=Directory Manager",
        bindDnPassword = "${directorioBackoffice.claveDirectoryManager}",
        callerBaseDn = "ou=usuarios,dc=carga,dc=uy",
        callerNameAttribute = "uid",
        groupSearchBase = "ou=grupos,dc=carga,dc=uy",
        groupMemberAttribute = "member",
        groupNameAttribute = "cn")
public class DirectorioBackoffice {

    @Inject
    IdentityStoreHandler identityStoreHandler;

    @EJB
    UsuarioEJBLocal usuarioEJB;

    public TokenDto ingresar(String usuario, String clave) {
        CredentialValidationResult resultado = identityStoreHandler.validate(
                new UsernamePasswordCredential(usuario, clave));
        if (resultado.getStatus() != CredentialValidationResult.Status.VALID) {
            throw new CargaUYException(CodigoError.AUTH_CREDENCIALES_INVALIDAS);
        }
        UsuarioDto backoffice = usuarioEJB.loginBackoffice(resultado.getCallerPrincipal().getName(),
                resultado.getCallerGroups());
        return usuarioEJB.crearToken(backoffice);
    }

    public String getUrl() {
        return System.getenv().getOrDefault("LDAP_URL", "ldap://ldap:3389");
    }

    public String getClaveDirectoryManager() {
        return System.getenv("LDAP_DM_PASSWORD");
    }
}
