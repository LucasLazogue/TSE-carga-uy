package tse.cargauy.negocio.ldap;

import java.util.Set;

public class IdentidadLdap {

    private final String usuario;
    private final Set<String> grupos;

    public IdentidadLdap(String usuario, Set<String> grupos) {
        this.usuario = usuario;
        this.grupos = grupos;
    }

    public String getUsuario() {
        return usuario;
    }

    public Set<String> getGrupos() {
        return grupos;
    }
}
