package tse.cargauy.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Rol {

    public static final String CIUDADANO = "CIUDADANO";
    public static final String CHOFER = "CHOFER";
    public static final String RESPONSABLE = "RESPONSABLE";
    public static final String FUNCIONARIO = "FUNCIONARIO";

    @Id @GeneratedValue
    private Long id;
    @Column(unique = true, nullable = false)
    private String nombre;

    public Rol() {
    }

    public Rol(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

}
