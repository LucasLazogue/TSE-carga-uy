package tse.cargauy.dtos;

import java.io.Serializable;

public class FiltroVehiculos implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idEmpresa;
    private Integer capacidadMinima;

    public FiltroVehiculos() {
    }

    public FiltroVehiculos(Long idEmpresa, Integer capacidadMinima) {
        this.idEmpresa = idEmpresa;
        this.capacidadMinima = capacidadMinima;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public Integer getCapacidadMinima() {
        return capacidadMinima;
    }

    public void setCapacidadMinima(Integer capacidadMinima) {
        this.capacidadMinima = capacidadMinima;
    }
}
