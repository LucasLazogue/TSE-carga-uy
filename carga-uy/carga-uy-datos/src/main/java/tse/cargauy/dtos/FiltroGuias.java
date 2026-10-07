package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

public class FiltroGuias implements Serializable {

    private static final long serialVersionUID = 1L;

    private String busqueda;
    private Boolean conViaje;
    private LocalDate desde;
    private LocalDate hasta;
    private List<Long> idsEmpresa;

    public FiltroGuias() {
    }

    public FiltroGuias(String busqueda, Boolean conViaje, LocalDate desde, LocalDate hasta) {
        this.busqueda = busqueda;
        this.conViaje = conViaje;
        this.desde = desde;
        this.hasta = hasta;
    }

    public String getBusqueda() {
        return busqueda;
    }

    public void setBusqueda(String busqueda) {
        this.busqueda = busqueda;
    }

    public Boolean getConViaje() {
        return conViaje;
    }

    public void setConViaje(Boolean conViaje) {
        this.conViaje = conViaje;
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

    public List<Long> getIdsEmpresa() {
        return idsEmpresa;
    }

    public void setIdsEmpresa(List<Long> idsEmpresa) {
        this.idsEmpresa = idsEmpresa;
    }
}
