package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;

public class ItvDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long idVehiculo;
    private LocalDate fechaInspeccion;
    private LocalDate fechaVencimiento;

    public ItvDto() {
    }

    public ItvDto(Long idVehiculo, LocalDate fechaInspeccion, LocalDate fechaVencimiento) {
        this.idVehiculo = idVehiculo;
        this.fechaInspeccion = fechaInspeccion;
        this.fechaVencimiento = fechaVencimiento;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdVehiculo() {
        return idVehiculo;
    }

    public void setIdVehiculo(Long idVehiculo) {
        this.idVehiculo = idVehiculo;
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
