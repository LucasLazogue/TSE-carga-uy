package tse.cargauy.web.ws.guias;

import java.time.LocalDate;

import jakarta.ws.rs.QueryParam;
import tse.cargauy.dtos.FiltroGuias;

public class GuiasQuery {

    @QueryParam("busqueda")
    private String busqueda;

    @QueryParam("conViaje")
    private Boolean conViaje;

    @QueryParam("desde")
    private LocalDate desde;

    @QueryParam("hasta")
    private LocalDate hasta;

    public FiltroGuias filtro() {
        return new FiltroGuias(busqueda, conViaje, desde, hasta);
    }
}
