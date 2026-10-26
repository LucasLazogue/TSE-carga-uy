package tse.cargauy.entities;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

// una fila por inspeccion: al renovar se agrega otra y queda el historico
@Entity
public class Itv {

    @Id @GeneratedValue
    private Long id;
    @ManyToOne(optional = false)
    private Vehiculo vehiculo;
    @Column(nullable = false)
    private LocalDate fechaInspeccion;
    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    public Itv() {
    }

    public Itv(Vehiculo vehiculo, LocalDate fechaInspeccion, LocalDate fechaVencimiento) {
        this.vehiculo = vehiculo;
        this.fechaInspeccion = fechaInspeccion;
        this.fechaVencimiento = fechaVencimiento;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(Vehiculo vehiculo) {
        this.vehiculo = vehiculo;
    }

    public LocalDate getFechaInspeccion() {
        return fechaInspeccion;
    }

    public void setFechaInspeccion(LocalDate fechaInspeccion) {
        this.fechaInspeccion = fechaInspeccion;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

}
