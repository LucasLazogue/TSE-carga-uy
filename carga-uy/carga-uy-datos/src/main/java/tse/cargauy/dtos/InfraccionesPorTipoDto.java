package tse.cargauy.dtos;

import java.io.Serializable;

import tse.cargauy.entities.TipoInfraccion;

public class InfraccionesPorTipoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private TipoInfraccion tipo;
    private long detectadas;
    private long confirmadas;
    private long desestimadas;

    public InfraccionesPorTipoDto() {
    }

    public InfraccionesPorTipoDto(TipoInfraccion tipo, long detectadas, long confirmadas, long desestimadas) {
        this.tipo = tipo;
        this.detectadas = detectadas;
        this.confirmadas = confirmadas;
        this.desestimadas = desestimadas;
    }

    public long getPendientes() {
        return detectadas - confirmadas - desestimadas;
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

    public long getDesestimadas() {
        return desestimadas;
    }

    public void setDesestimadas(long desestimadas) {
        this.desestimadas = desestimadas;
    }
}
