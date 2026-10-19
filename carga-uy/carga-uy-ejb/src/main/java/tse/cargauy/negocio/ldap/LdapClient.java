package tse.cargauy.negocio.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.LDAPURL;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.RDN;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.SingleServerSet;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.ldap.sdk.controls.VirtualListViewRequestControl;
import com.unboundid.ldap.sdk.controls.VirtualListViewResponseControl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStoreHandler;
import jakarta.security.enterprise.identitystore.LdapIdentityStoreDefinition;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@Named
@ApplicationScoped
@LdapIdentityStoreDefinition(
        url = "${ldapClient.url}",
        bindDn = ConstantesLdap.DIRECTORY_MANAGER_DN,
        bindDnPassword = "${ldapClient.claveDirectoryManager}",
        callerBaseDn = ConstantesLdap.USERS_DN,
        callerNameAttribute = ConstantesLdap.USER_ID,
        groupSearchBase = ConstantesLdap.GROUPS_DN,
        groupMemberAttribute = ConstantesLdap.MEMBER,
        groupNameAttribute = ConstantesLdap.COMMON_NAME)
public class LdapClient {

    private static final Logger LOGGER = Logger.getLogger(LdapClient.class.getName());
    private static final int TIMEOUT_MS = 3000;

    @Inject
    IdentityStoreHandler identityStoreHandler;

    LDAPInterface directorio;

    @PostConstruct
    void init() {
        try {
            LDAPURL url = new LDAPURL(getUrl());
            LDAPConnectionOptions opciones = new LDAPConnectionOptions();
            opciones.setConnectTimeoutMillis(TIMEOUT_MS);
            opciones.setResponseTimeoutMillis(TIMEOUT_MS);
            directorio = new LDAPConnectionPool(new SingleServerSet(url.getHost(), url.getPort(), opciones),
                    new SimpleBindRequest(ConstantesLdap.DIRECTORY_MANAGER_DN, getClaveDirectoryManager()),
                    0, 5, 1, null, false);
        } catch (LDAPException e) {
            throw new IllegalStateException(e);
        }
    }

    @PreDestroy
    void close() {
        if (directorio instanceof LDAPConnectionPool pool) {
            pool.close();
        }
    }

    @Timeout(TIMEOUT_MS)
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

    public PaginaDto<UsuarioDto> getUsuarios(Paginacion paginacion) {
        try {
            SearchRequest busqueda = new SearchRequest(ConstantesLdap.USERS_DN, SearchScope.ONE,
                    Filter.createEqualityFilter(ConstantesLdap.OBJECT_CLASS, ConstantesLdap.INET_ORG_PERSON),
                    ConstantesLdap.USER_ID);
            busqueda.addControl(new ServerSideSortRequestControl(new SortKey(ConstantesLdap.USER_ID)));
            busqueda.addControl(new VirtualListViewRequestControl(paginacion.getDesde() + 1, 0,
                    paginacion.getTamanio() - 1, 0, null));
            SearchResult resultado = directorio.search(busqueda);
            List<SearchResultEntry> entradas = resultado.getSearchEntries();
            Map<DN, List<String>> roles = getRolesPorUsuario(entradas);
            List<UsuarioDto> usuarios = new ArrayList<>();
            for (SearchResultEntry entrada : entradas) {
                usuarios.add(new UsuarioDto(entrada.getAttributeValue(ConstantesLdap.USER_ID),
                        roles.getOrDefault(entrada.getParsedDN(), List.of())));
            }
            int total = VirtualListViewResponseControl.get(resultado).getContentCount();
            return new PaginaDto<>(usuarios, total, paginacion.getPagina(), paginacion.getTamanio());
        } catch (LDAPException e) {
            throw error(e, null);
        }
    }

    public void addUsuario(UsuarioDto usuario, String clave) {
        String dn = getDn(usuario.getUsuario());
        try {
            directorio.add(new Entry(dn,
                    new Attribute(ConstantesLdap.OBJECT_CLASS, ConstantesLdap.INET_ORG_PERSON),
                    new Attribute(ConstantesLdap.USER_ID, usuario.getUsuario()),
                    new Attribute(ConstantesLdap.COMMON_NAME, usuario.getUsuario()),
                    new Attribute(ConstantesLdap.SURNAME, usuario.getUsuario()),
                    new Attribute(ConstantesLdap.USER_PASSWORD, clave)));
            for (String rol : usuario.getRoles()) {
                addMiembro(rol, dn);
            }
        } catch (LDAPException e) {
            throw error(e, usuario.getUsuario());
        }
    }

    public void updateUsuario(UsuarioDto usuario) {
        String dn = getDn(usuario.getUsuario());
        try {
            if (directorio.getEntry(dn, SearchRequest.NO_ATTRIBUTES) == null) {
                throw new CargaUYException(CodigoError.USUARIO_BACKOFFICE_NO_ENCONTRADO, usuario.getUsuario());
            }
            List<String> actuales = getRoles(dn);
            for (String rol : usuario.getRoles()) {
                if (!actuales.contains(rol)) {
                    addMiembro(rol, dn);
                }
            }
            for (String rol : actuales) {
                if (!usuario.getRoles().contains(rol)) {
                    deleteMiembro(rol, dn);
                }
            }
        } catch (LDAPException e) {
            throw error(e, usuario.getUsuario());
        }
    }

    public void updateClave(String usuario, String clave) {
        try {
            directorio.modify(getDn(usuario),
                    new Modification(ModificationType.REPLACE, ConstantesLdap.USER_PASSWORD, clave));
        } catch (LDAPException e) {
            throw error(e, usuario);
        }
    }

    public void deleteUsuario(String usuario) {
        String dn = getDn(usuario);
        try {
            for (String rol : getRoles(dn)) {
                deleteMiembro(rol, dn);
            }
            directorio.delete(dn);
        } catch (LDAPException e) {
            throw error(e, usuario);
        }
    }

    public String getUrl() {
        return System.getenv().getOrDefault("LDAP_URL", "ldap://ldap:3389");
    }

    public String getClaveDirectoryManager() {
        return System.getenv("LDAP_DM_PASSWORD");
    }

    private List<String> getRoles(String dn) throws LDAPException {
        List<String> roles = new ArrayList<>();
        for (SearchResultEntry grupo : directorio.search(ConstantesLdap.GROUPS_DN, SearchScope.ONE,
                Filter.createEqualityFilter(ConstantesLdap.MEMBER, dn), ConstantesLdap.COMMON_NAME).getSearchEntries()) {
            roles.add(grupo.getAttributeValue(ConstantesLdap.COMMON_NAME).toUpperCase(Locale.ROOT));
        }
        return roles;
    }

    private Map<DN, List<String>> getRolesPorUsuario(List<SearchResultEntry> usuarios) throws LDAPException {
        Map<DN, List<String>> roles = new HashMap<>();
        if (usuarios.isEmpty()) {
            return roles;
        }
        List<Filter> miembros = new ArrayList<>();
        for (SearchResultEntry usuario : usuarios) {
            miembros.add(Filter.createEqualityFilter(ConstantesLdap.MEMBER, usuario.getDN()));
        }
        for (SearchResultEntry grupo : directorio.search(ConstantesLdap.GROUPS_DN, SearchScope.ONE,
                Filter.createORFilter(miembros), ConstantesLdap.COMMON_NAME, ConstantesLdap.MEMBER).getSearchEntries()) {
            String rol = grupo.getAttributeValue(ConstantesLdap.COMMON_NAME).toUpperCase(Locale.ROOT);
            for (String miembro : grupo.getAttributeValues(ConstantesLdap.MEMBER)) {
                roles.computeIfAbsent(new DN(miembro), dn -> new ArrayList<>()).add(rol);
            }
        }
        return roles;
    }

    private void addMiembro(String rol, String dn) throws LDAPException {
        directorio.modify(getDnGrupo(rol), new Modification(ModificationType.ADD, ConstantesLdap.MEMBER, dn));
    }

    private void deleteMiembro(String rol, String dn) throws LDAPException {
        directorio.modify(getDnGrupo(rol), new Modification(ModificationType.DELETE, ConstantesLdap.MEMBER, dn));
    }

    private static String getDn(String usuario) {
        return new RDN(ConstantesLdap.USER_ID, usuario) + "," + ConstantesLdap.USERS_DN;
    }

    private static String getDnGrupo(String rol) {
        return new RDN(ConstantesLdap.COMMON_NAME, rol.toLowerCase(Locale.ROOT)) + "," + ConstantesLdap.GROUPS_DN;
    }

    private static CargaUYException error(LDAPException e, String usuario) {
        if (e.getResultCode() == ResultCode.ENTRY_ALREADY_EXISTS) {
            return new CargaUYException(CodigoError.USUARIO_BACKOFFICE_DUPLICADO, usuario);
        }
        if (e.getResultCode() == ResultCode.NO_SUCH_OBJECT) {
            return new CargaUYException(CodigoError.USUARIO_BACKOFFICE_NO_ENCONTRADO, usuario);
        }
        LOGGER.warning("directorio usuario=" + usuario + " resultado=" + e.getResultCode() + " error=" + e.getMessage());
        return new CargaUYException(CodigoError.AUTH_DIRECTORIO_NO_DISPONIBLE);
    }
}
