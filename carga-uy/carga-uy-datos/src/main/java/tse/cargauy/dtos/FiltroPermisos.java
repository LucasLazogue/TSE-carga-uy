package tse.cargauy.dtos;

import java.io.Serializable;

import tse.cargauy.entities.EstadoPermiso;

public class FiltroPermisos implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idEmpresa;
    private Long idVehiculo;
    private EstadoPermiso estado;

    public FiltroPermisos() {
    }

    public FiltroPermisos(Long idEmpresa, Long idVehiculo, EstadoPermiso estado) {
        this.idEmpresa = idEmpresa;
        this.idVehiculo = idVehiculo;
        this.estado = estado;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
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
