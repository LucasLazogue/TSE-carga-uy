package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;

public class SolicitudPermisoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nroPermiso;
    private LocalDate validoDesde;
    private Long idVehiculo;

    public SolicitudPermisoDto() {
    }

    public SolicitudPermisoDto(String nroPermiso, LocalDate validoDesde, Long idVehiculo) {
        this.nroPermiso = nroPermiso;
        this.validoDesde = validoDesde;
        this.idVehiculo = idVehiculo;
    }

    public String getNroPermiso() {
        return nroPermiso;
    }

    public void setNroPermiso(String nroPermiso) {
        this.nroPermiso = nroPermiso;
    }

    public LocalDate getValidoDesde() {
        return validoDesde;
    }

    public void setValidoDesde(LocalDate validoDesde) {
        this.validoDesde = validoDesde;
    }

    public Long getIdVehiculo() {
        return idVehiculo;
    }

    public void setIdVehiculo(Long idVehiculo) {
        this.idVehiculo = idVehiculo;
    }
}
