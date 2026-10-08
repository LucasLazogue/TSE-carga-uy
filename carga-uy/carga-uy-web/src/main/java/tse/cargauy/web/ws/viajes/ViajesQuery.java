package tse.cargauy.web.ws.viajes;

import jakarta.ws.rs.QueryParam;
import tse.cargauy.dtos.FiltroViajes;
import tse.cargauy.entities.EstadoViaje;

public class ViajesQuery {

    @QueryParam("idChofer")
    private Long idChofer;

    @QueryParam("idVehiculo")
    private Long idVehiculo;

    @QueryParam("estado")
    private EstadoViaje estado;

    public FiltroViajes filtro() {
        return new FiltroViajes(idChofer, idVehiculo, estado);
    }
}
