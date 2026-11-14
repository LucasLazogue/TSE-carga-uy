package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.YearMonth;

public class ViajesPorMesPublicoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String mes;
    private Long cantidad;

    public ViajesPorMesPublicoDto() {
    }

    public ViajesPorMesPublicoDto(String mes, Long cantidad) {
        this.mes = mes;
        this.cantidad = cantidad;
    }

    public ViajesPorMesPublicoDto(Integer anio, Integer mes, Long cantidad) {
        this(YearMonth.of(anio, mes).toString(), cantidad);
    }

    public String getMes() {
        return mes;
    }

    public void setMes(String mes) {
        this.mes = mes;
    }

    public Long getCantidad() {
        return cantidad;
    }

    public void setCantidad(Long cantidad) {
        this.cantidad = cantidad;
    }
}
