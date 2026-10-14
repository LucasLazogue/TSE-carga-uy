package tse.cargauy.web.beans;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import tse.cargauy.dtos.TokenDto;
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
        TokenDto token = usuarioEJB.crearToken(usuarioEJB.loginBackoffice(usuario, clave));
        ExternalContext contexto = FacesContext.getCurrentInstance().getExternalContext();
        Cookie cookie = new Cookie(COOKIE_SESION, token.getToken());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(contexto.isSecure());
        ((HttpServletResponse) contexto.getResponse()).addCookie(cookie);
        return "nodos?faces-redirect=true";
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
