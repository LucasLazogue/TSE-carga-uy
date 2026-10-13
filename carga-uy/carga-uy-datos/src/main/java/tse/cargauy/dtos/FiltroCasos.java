package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.TipoInfraccion;

public class FiltroCasos implements Serializable {

    private static final long serialVersionUID = 1L;

    private EstadoCaso estado;
    private TipoInfraccion tipo;
    private LocalDate desde;
    private LocalDate hasta;
    private List<Long> idsEmpresa;
    private boolean soloNotificados;

    public FiltroCasos() {
    }

    public FiltroCasos(EstadoCaso estado, TipoInfraccion tipo, LocalDate desde, LocalDate hasta) {
        this.estado = estado;
        this.tipo = tipo;
        this.desde = desde;
        this.hasta = hasta;
    }

    public EstadoCaso getEstado() {
        return estado;
    }

    public void setEstado(EstadoCaso estado) {
        this.estado = estado;
    }

    public TipoInfraccion getTipo() {
        return tipo;
    }

    public void setTipo(TipoInfraccion tipo) {
        this.tipo = tipo;
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

    public boolean isSoloNotificados() {
        return soloNotificados;
    }

    public void setSoloNotificados(boolean soloNotificados) {
        this.soloNotificados = soloNotificados;
    }
}
