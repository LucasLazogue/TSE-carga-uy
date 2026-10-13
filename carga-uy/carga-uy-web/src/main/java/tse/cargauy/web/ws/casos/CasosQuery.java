package tse.cargauy.web.ws.casos;

import java.time.LocalDate;
import java.util.List;

import jakarta.ws.rs.QueryParam;
import tse.cargauy.dtos.FiltroCasos;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.TipoInfraccion;

public class CasosQuery {

    @QueryParam("estado")
    private EstadoCaso estado;

    @QueryParam("tipo")
    private TipoInfraccion tipo;

    @QueryParam("empresa")
    private Long idEmpresa;

    @QueryParam("desde")
    private LocalDate desde;

    @QueryParam("hasta")
    private LocalDate hasta;

    public FiltroCasos filtro() {
        FiltroCasos filtro = new FiltroCasos(estado, tipo, desde, hasta);
        if (idEmpresa != null) {
            filtro.setIdsEmpresa(List.of(idEmpresa));
        }
        return filtro;
    }
}
