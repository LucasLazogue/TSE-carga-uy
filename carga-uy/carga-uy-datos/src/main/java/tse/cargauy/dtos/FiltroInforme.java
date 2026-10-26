package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;

public class FiltroInforme implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate desde;
    private LocalDate hasta;
    private Long idTipoCarga;

    public FiltroInforme() {
    }

    public FiltroInforme(LocalDate desde, LocalDate hasta, Long idTipoCarga) {
        this.desde = desde;
        this.hasta = hasta;
        this.idTipoCarga = idTipoCarga;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public void setDesde(LocalDate desde) {
        this.desde = desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public void setHasta(LocalDate hasta) {
        this.hasta = hasta;
    }

    public Long getIdTipoCarga() {
        return idTipoCarga;
    }

    public void setIdTipoCarga(Long idTipoCarga) {
        this.idTipoCarga = idTipoCarga;
    }
}
