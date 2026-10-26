package tse.cargauy.dtos;

import java.io.Serializable;

import tse.cargauy.entities.TipoInfraccion;

public class InfraccionesPorTipoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private TipoInfraccion tipo;
    private long detectadas;
    private long confirmadas;
    private long pendientes;

    public InfraccionesPorTipoDto() {
    }

    public InfraccionesPorTipoDto(TipoInfraccion tipo) {
        this.tipo = tipo;
    }

    public TipoInfraccion getTipo() {
        return tipo;
    }

    public void setTipo(TipoInfraccion tipo) {
        this.tipo = tipo;
    }

    public long getDetectadas() {
        return detectadas;
    }

    public void setDetectadas(long detectadas) {
        this.detectadas = detectadas;
    }

    public long getConfirmadas() {
        return confirmadas;
    }

    public void setConfirmadas(long confirmadas) {
        this.confirmadas = confirmadas;
    }

    public long getPendientes() {
        return pendientes;
    }

    public void setPendientes(long pendientes) {
        this.pendientes = pendientes;
    }
}
