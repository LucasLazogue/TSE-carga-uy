package tse.cargauy.dtos;

import java.io.Serializable;
import java.util.List;

import tse.cargauy.entities.EstadoViaje;

public class FiltroViajes implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idChofer;
    private Long idVehiculo;
    private EstadoViaje estado;
    private List<Long> idsEmpresa;

    public FiltroViajes() {
    }

    public FiltroViajes(Long idChofer, Long idVehiculo, EstadoViaje estado) {
        this.idChofer = idChofer;
        this.idVehiculo = idVehiculo;
        this.estado = estado;
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

    public List<Long> getIdsEmpresa() {
        return idsEmpresa;
    }

    public void setIdsEmpresa(List<Long> idsEmpresa) {
        this.idsEmpresa = idsEmpresa;
    }
}
