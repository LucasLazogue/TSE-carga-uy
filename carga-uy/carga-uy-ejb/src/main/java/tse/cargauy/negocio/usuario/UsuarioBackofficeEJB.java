package tse.cargauy.negocio.usuario;

import java.util.List;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.entities.Rol;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.ldap.LdapClient;

@Stateless
@RolesAllowed(Rol.ADMINISTRADOR)
public class UsuarioBackofficeEJB implements UsuarioBackofficeEJBLocal {

    @EJB
    UsuarioDAOLocal usuarioDAO;

    @Inject
    LdapClient ldap;

    @Resource
    SessionContext contexto;

    public List<UsuarioDto> getUsuarios() {
        return ldap.getUsuarios();
    }

    public void addUsuario(UsuarioDto usuario, String clave) {
        validarUsuario(usuario);
        if (clave == null || clave.isBlank()) {
            throw new CargaUYException(CodigoError.USUARIO_BACKOFFICE_CLAVE_REQUERIDA);
        }
        ldap.addUsuario(usuario, clave);
    }

    public void updateUsuario(UsuarioDto usuario, String clave) {
        validarUsuario(usuario);
        if (usuario.getUsuario().equals(getUsuarioSesion()) && !usuario.getRoles().contains(Rol.ADMINISTRADOR)) {
            throw new CargaUYException(CodigoError.USUARIO_BACKOFFICE_PROPIO);
        }
        ldap.updateUsuario(usuario);
        if (clave != null && !clave.isBlank()) {
            ldap.updateClave(usuario.getUsuario(), clave);
        }
    }

    public void deleteUsuario(String usuario) {
        if (usuario.equals(getUsuarioSesion())) {
            throw new CargaUYException(CodigoError.USUARIO_BACKOFFICE_PROPIO);
        }
        usuarioDAO.deleteUsuarioBackoffice(usuario);
        ldap.deleteUsuario(usuario);
    }

    String getUsuarioSesion() {
        Long id = Long.valueOf(contexto.getCallerPrincipal().getName());
        return usuarioDAO.getById(id).getUsuario();
    }

    private static void validarUsuario(UsuarioDto usuario) {
        if (usuario.getUsuario() == null || usuario.getUsuario().isBlank()) {
            throw new CargaUYException(CodigoError.USUARIO_BACKOFFICE_USUARIO_REQUERIDO);
        }
        if (usuario.getRoles() == null || usuario.getRoles().isEmpty()
                || !Rol.BACKOFFICE.containsAll(usuario.getRoles())) {
            throw new CargaUYException(CodigoError.USUARIO_BACKOFFICE_ROL_REQUERIDO);
        }
    }
}
