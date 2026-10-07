package tse.cargauy.entities;

import java.time.LocalDate;

import jakarta.persistence.Entity;

@Entity
public class Responsable extends VinculoEmpresa {

    public Responsable() {
    }

    public Responsable(Ciudadano ciudadano, Empresa empresa, LocalDate fechaDesde) {
        super(ciudadano, empresa, fechaDesde);
    }

}
