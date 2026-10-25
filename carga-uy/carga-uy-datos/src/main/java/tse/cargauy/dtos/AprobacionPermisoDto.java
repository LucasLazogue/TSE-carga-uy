package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;

public class AprobacionPermisoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate validoDesde;
    private LocalDate validoHasta;

    public AprobacionPermisoDto() {
    }

    public AprobacionPermisoDto(LocalDate validoDesde, LocalDate validoHasta) {
        this.validoDesde = validoDesde;
        this.validoHasta = validoHasta;
    }

    public LocalDate getValidoDesde() {
        return validoDesde;
    }

    public void setValidoDesde(LocalDate validoDesde) {
        this.validoDesde = validoDesde;
    }

    public LocalDate getValidoHasta() {
        return validoHasta;
    }

    public void setValidoHasta(LocalDate validoHasta) {
        this.validoHasta = validoHasta;
    }
}
