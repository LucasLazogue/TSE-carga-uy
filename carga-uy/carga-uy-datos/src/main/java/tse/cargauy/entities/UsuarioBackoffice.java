package tse.cargauy.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class UsuarioBackoffice extends Usuario {

    @Column(unique = true, nullable = false)
    private String usuario;

    public UsuarioBackoffice() {
    }

    public UsuarioBackoffice(String usuario) {
        this.usuario = usuario;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
}
