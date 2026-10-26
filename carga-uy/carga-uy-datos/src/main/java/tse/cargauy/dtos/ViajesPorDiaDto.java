package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;

public class ViajesPorDiaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate dia;
    private long cantidad;

    public ViajesPorDiaDto() {
    }

    public ViajesPorDiaDto(LocalDate dia, long cantidad) {
        this.dia = dia;
        this.cantidad = cantidad;
    }

    public LocalDate getDia() {
        return dia;
    }

    public void setDia(LocalDate dia) {
        this.dia = dia;
    }

    public long getCantidad() {
        return cantidad;
    }

    public void setCantidad(long cantidad) {
        this.cantidad = cantidad;
    }
}
