package tse.cargauy.web.ws.vehiculos;

import jakarta.ws.rs.QueryParam;
import tse.cargauy.dtos.FiltroVehiculos;

public class VehiculosQuery {

    @QueryParam("idEmpresa")
    private Long idEmpresa;

    @QueryParam("capacidadMinima")
    private Integer capacidadMinima;

    public FiltroVehiculos filtro() {
        return new FiltroVehiculos(idEmpresa, capacidadMinima);
    }
}
