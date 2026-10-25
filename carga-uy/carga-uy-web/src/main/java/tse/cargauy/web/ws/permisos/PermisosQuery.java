package tse.cargauy.web.ws.permisos;

import jakarta.ws.rs.QueryParam;
import tse.cargauy.dtos.FiltroPermisos;
import tse.cargauy.entities.EstadoPermiso;

public class PermisosQuery {

    @QueryParam("idEmpresa")
    private Long idEmpresa;

    @QueryParam("idVehiculo")
    private Long idVehiculo;

    @QueryParam("estado")
    private EstadoPermiso estado;

    public FiltroPermisos filtro() {
        return new FiltroPermisos(idEmpresa, idVehiculo, estado);
    }
}
