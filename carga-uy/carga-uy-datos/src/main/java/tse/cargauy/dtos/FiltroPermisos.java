package tse.cargauy.dtos;

import java.io.Serializable;

import tse.cargauy.entities.EstadoPermiso;

public class FiltroPermisos implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idVehiculo;
    private EstadoPermiso estado;

    public FiltroPermisos() {
    }

    public FiltroPermisos(Long idVehiculo, EstadoPermiso estado) {
        this.idVehiculo = idVehiculo;
        this.estado = estado;
    }

    public Long getIdVehiculo() {
        return idVehiculo;
    }

    public void setIdVehiculo(Long idVehiculo) {
        this.idVehiculo = idVehiculo;
    }

    public EstadoPermiso getEstado() {
        return estado;
    }

    public void setEstado(EstadoPermiso estado) {
        this.estado = estado;
    }
}
