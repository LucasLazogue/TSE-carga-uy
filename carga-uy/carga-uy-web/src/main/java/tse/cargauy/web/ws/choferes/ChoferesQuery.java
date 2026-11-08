package tse.cargauy.web.ws.choferes;

import java.time.LocalDate;

import jakarta.ws.rs.QueryParam;
import tse.cargauy.dtos.FiltroChoferes;

public class ChoferesQuery {

    @QueryParam("cedula")
    private String cedula;

    @QueryParam("fecha")
    private LocalDate fecha;

    @QueryParam("historial")
    private boolean historial;

    public FiltroChoferes filtro() {
        return new FiltroChoferes(cedula, fecha, historial);
    }
}
