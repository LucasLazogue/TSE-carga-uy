package tse.cargauy.web.beans;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

/**
 * a donde manda la pagina de vista expirada. el login es un JWT en cookie y sigue valido aunque la pantalla no se haya
 * podido recuperar, asi que se vuelve a la pantalla de inicio del rol, no al login
 */
@Named
@RequestScoped
public class VistaExpiradaView {

    public String getDestino() {
        return PantallaInicio.para(FacesContext.getCurrentInstance().getExternalContext()::isUserInRole);
    }

    public boolean isConSesion() {
        return !PantallaInicio.LOGIN.equals(getDestino());
    }
}
