package tse.cargauy.entities;

import jakarta.persistence.Entity;

@Entity
public class Chofer extends Ciudadano {

    public Chofer() {
    }

    public Chofer(String cedula, String correo) {
        super(cedula, correo);
    }

}
