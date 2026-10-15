package tse.cargauy.web.beans;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import tse.cargauy.dtos.TokenDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.entities.Rol;
import tse.cargauy.negocio.usuario.UsuarioEJBLocal;

@Named
@RequestScoped
public class LoginView {

    private static final String COOKIE_SESION = "cargauy_token";

    @EJB
    private UsuarioEJBLocal usuarioEJB;

    private String usuario;
    private String clave;

    public String ingresar() {
        UsuarioDto backoffice = usuarioEJB.loginBackoffice(usuario, clave);
        TokenDto token = usuarioEJB.crearToken(backoffice);
        FacesContext faces = FacesContext.getCurrentInstance();
        ExternalContext contexto = faces.getExternalContext();
        Cookie cookie = new Cookie(COOKIE_SESION, token.getToken());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(contexto.isSecure());
        ((HttpServletResponse) contexto.getResponse()).addCookie(cookie);
        if (backoffice.getRoles().contains(Rol.ADMINISTRADOR)) {
            return "/admin/nodos?faces-redirect=true";
        }
        faces.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Ingresaste como autoridad. El tablero de reportes todavia no esta disponible.", null));
        return null;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }
}
