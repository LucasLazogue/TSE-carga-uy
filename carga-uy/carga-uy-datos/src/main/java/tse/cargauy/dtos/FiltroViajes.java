package tse.cargauy.dtos;

import java.io.Serializable;

import tse.cargauy.entities.EstadoViaje;

public class FiltroViajes implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idEmpresa;
    private Long idChofer;
    private Long idVehiculo;
    private EstadoViaje estado;

    public FiltroViajes() {
    }

    public FiltroViajes(Long idEmpresa, Long idChofer, Long idVehiculo, EstadoViaje estado) {
        this.idEmpresa = idEmpresa;
        this.idChofer = idChofer;
        this.idVehiculo = idVehiculo;
        this.estado = estado;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public Long getIdChofer() {
        return idChofer;
    }

    public void setIdChofer(Long idChofer) {
        this.idChofer = idChofer;
    }

    public Long getIdVehiculo() {
        return idVehiculo;
    }

    public void setIdVehiculo(Long idVehiculo) {
        this.idVehiculo = idVehiculo;
    }

    public EstadoViaje getEstado() {
        return estado;
    }

    public void setEstado(EstadoViaje estado) {
        this.estado = estado;
    }
}
