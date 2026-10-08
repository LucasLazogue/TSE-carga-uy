package tse.cargauy.web.ws;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import tse.cargauy.dtos.Paginacion;

public class PaginacionQuery {

    @QueryParam("pagina")
    @DefaultValue("0")
    private int pagina;

    @QueryParam("tamanio")
    @DefaultValue("20")
    private int tamanio;

    public Paginacion paginacion() {
        return new Paginacion(pagina, tamanio);
    }
}
