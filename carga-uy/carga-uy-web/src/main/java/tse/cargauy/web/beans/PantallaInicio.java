package tse.cargauy.web.beans;

import java.util.function.Predicate;

import tse.cargauy.entities.Rol;

/** pantalla de inicio del backoffice segun los roles del usuario */
public final class PantallaInicio {

    static final String LOGIN = "/login";

    private PantallaInicio() {
    }

    /** la del administrador si lo es, si no la de la autoridad; sin rol del backoffice, el login */
    public static String para(Predicate<String> tieneRol) {
        if (tieneRol.test(Rol.ADMINISTRADOR)) {
            return "/admin/nodos";
        }
        if (tieneRol.test(Rol.AUTORIDAD)) {
            return "/autoridad/reportes";
        }
        return LOGIN;
    }
}
