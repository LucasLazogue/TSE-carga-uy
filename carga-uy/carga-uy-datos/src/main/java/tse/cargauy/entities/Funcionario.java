package tse.cargauy.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Funcionario extends Usuario {

    @Column(unique = true, nullable = false)
    private String cedula;

    public Funcionario() {
    }

    public Funcionario(String cedula) {
        this.cedula = cedula;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

}
