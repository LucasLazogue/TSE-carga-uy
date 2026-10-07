package tse.cargauy.web.ws.viajes;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import tse.cargauy.dtos.FiltroViajes;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.web.ws.Parametros;

public class ViajesQuery {

    @QueryParam("idEmpresa")
    private Long idEmpresa;

    @QueryParam("idChofer")
    private Long idChofer;

    @QueryParam("idVehiculo")
    private Long idVehiculo;

    @QueryParam("estado")
    private String estado;

    @QueryParam("pagina")
    @DefaultValue("0")
    private int pagina;

    @QueryParam("tamanio")
    @DefaultValue("20")
    private int tamanio;

    public FiltroViajes filtro() {
        return new FiltroViajes(idEmpresa, idChofer, idVehiculo,
                Parametros.enumeracion(EstadoViaje.class, "estado", estado));
    }

    public Paginacion paginacion() {
        return new Paginacion(pagina, tamanio);
    }
}
