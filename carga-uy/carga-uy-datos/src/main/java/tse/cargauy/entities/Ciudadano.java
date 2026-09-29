package tse.cargauy.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Ciudadano extends Usuario {

    @Column(unique = true, nullable = false)
    private String cedula;
    private String correo;

    public Ciudadano() {
    }

    public Ciudadano(String cedula, String correo) {
        this.cedula = cedula;
        this.correo = correo;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

}
