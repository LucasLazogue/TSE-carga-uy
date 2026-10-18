package tse.cargauy.web.beans;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import tse.cargauy.negocio.usuario.UsuarioEJBLocal;

@Named
@RequestScoped
public class SesionView {

    private static final String COOKIE_SESION = "cargauy_token";

    @EJB
    private UsuarioEJBLocal usuarioEJB;

    private String usuario;

    public String getUsuario() {
        if (usuario == null) {
            usuario = usuarioEJB.getUsuarioSesion().getUsuario();
        }
        return usuario;
    }

    public String salir() {
        ExternalContext contexto = FacesContext.getCurrentInstance().getExternalContext();
        Cookie cookie = new Cookie(COOKIE_SESION, "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(contexto.isSecure());
        cookie.setMaxAge(0);
        ((HttpServletResponse) contexto.getResponse()).addCookie(cookie);
        contexto.invalidateSession();
        return "/login?faces-redirect=true";
    }
}
