package tse.cargauy.web.ws.guias;

import java.time.LocalDate;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import tse.cargauy.dtos.FiltroGuias;
import tse.cargauy.dtos.Paginacion;

public class GuiasQuery {

    @QueryParam("busqueda")
    private String busqueda;

    @QueryParam("conViaje")
    private Boolean conViaje;

    @QueryParam("desde")
    private LocalDate desde;

    @QueryParam("hasta")
    private LocalDate hasta;

    @QueryParam("pagina")
    @DefaultValue("0")
    private int pagina;

    @QueryParam("tamanio")
    @DefaultValue("20")
    private int tamanio;

    public FiltroGuias filtro() {
        return new FiltroGuias(busqueda, conViaje, desde, hasta);
    }

    public Paginacion paginacion() {
        return new Paginacion(pagina, tamanio);
    }
}
