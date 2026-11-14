package tse.cargauy.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Ciudadano extends Usuario {

    @Column(unique = true, nullable = false)
    private String cedula;
    private String correo;
    // datos de la PDI, null mientras esten pendientes. la fecha es texto porque DNIC la puede informar parcial
    private String nombres;
    private String apellidos;
    private String fechaNacimiento;

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

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

}
