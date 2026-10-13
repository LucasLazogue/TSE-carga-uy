package tse.cargauy.dtos;

import java.io.Serializable;
import java.util.List;

public class SyncRespuestaDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<ResultadoEventoDto> resultados;
    private ViajeMovilDto viaje;

    public SyncRespuestaDto() {
    }

    public SyncRespuestaDto(List<ResultadoEventoDto> resultados, ViajeMovilDto viaje) {
        this.resultados = resultados;
        this.viaje = viaje;
    }

    public List<ResultadoEventoDto> getResultados() {
        return resultados;
    }

    public void setResultados(List<ResultadoEventoDto> resultados) {
        this.resultados = resultados;
    }

    public ViajeMovilDto getViaje() {
        return viaje;
    }

    public void setViaje(ViajeMovilDto viaje) {
        this.viaje = viaje;
    }
}
