package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;

public class FiltroChoferes implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cedula;
    private LocalDate fecha;
    private boolean historial;

    public FiltroChoferes() {
    }

    public FiltroChoferes(String cedula, LocalDate fecha, boolean historial) {
        this.cedula = cedula;
        this.fecha = fecha;
        this.historial = historial;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public boolean isHistorial() {
        return historial;
    }

    public void setHistorial(boolean historial) {
        this.historial = historial;
    }
}
