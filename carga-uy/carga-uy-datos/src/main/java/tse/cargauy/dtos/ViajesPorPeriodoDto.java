package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;

public class ViajesPorPeriodoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate inicio;
    private long cantidad;

    public ViajesPorPeriodoDto() {
    }

    public ViajesPorPeriodoDto(LocalDate inicio, long cantidad) {
        this.inicio = inicio;
        this.cantidad = cantidad;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public void setInicio(LocalDate inicio) {
        this.inicio = inicio;
    }

    public long getCantidad() {
        return cantidad;
    }

    public void setCantidad(long cantidad) {
        this.cantidad = cantidad;
    }
}
