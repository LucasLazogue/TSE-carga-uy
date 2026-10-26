package tse.cargauy.dtos;

import java.io.Serializable;
import java.util.List;

public class SyncRespuestaDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<ResultadoEventoDto> resultados;
    // los viajes del chofer como quedaron despues de aplicar los eventos: el celular los adopta (AC20)
    private List<ViajeMovilDto> viajes;

    public SyncRespuestaDto() {
    }

    public SyncRespuestaDto(List<ResultadoEventoDto> resultados, List<ViajeMovilDto> viajes) {
        this.resultados = resultados;
        this.viajes = viajes;
    }

    public List<ResultadoEventoDto> getResultados() {
        return resultados;
    }

    public void setResultados(List<ResultadoEventoDto> resultados) {
        this.resultados = resultados;
    }

    public List<ViajeMovilDto> getViajes() {
        return viajes;
    }

    public void setViajes(List<ViajeMovilDto> viajes) {
        this.viajes = viajes;
    }
}
