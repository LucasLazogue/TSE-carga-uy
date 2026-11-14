package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

public class IndicadoresPublicosDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private int umbral;
    private LocalDate habilitadosAl;
    private Long empresasHabilitadas;
    private Long vehiculosHabilitados;
    private List<ViajesPorMesPublicoDto> viajesPorMes = List.of();
    private List<VolumenPorRubroPublicoDto> volumenPorRubro = List.of();

    public IndicadoresPublicosDto() {
    }

    public int getUmbral() {
        return umbral;
    }

    public void setUmbral(int umbral) {
        this.umbral = umbral;
    }

    public LocalDate getHabilitadosAl() {
        return habilitadosAl;
    }

    public void setHabilitadosAl(LocalDate habilitadosAl) {
        this.habilitadosAl = habilitadosAl;
    }

    public Long getEmpresasHabilitadas() {
        return empresasHabilitadas;
    }

    public void setEmpresasHabilitadas(Long empresasHabilitadas) {
        this.empresasHabilitadas = empresasHabilitadas;
    }

    public Long getVehiculosHabilitados() {
        return vehiculosHabilitados;
    }

    public void setVehiculosHabilitados(Long vehiculosHabilitados) {
        this.vehiculosHabilitados = vehiculosHabilitados;
    }

    public List<ViajesPorMesPublicoDto> getViajesPorMes() {
        return viajesPorMes;
    }

    public void setViajesPorMes(List<ViajesPorMesPublicoDto> viajesPorMes) {
        this.viajesPorMes = viajesPorMes;
    }

    public List<VolumenPorRubroPublicoDto> getVolumenPorRubro() {
        return volumenPorRubro;
    }

    public void setVolumenPorRubro(List<VolumenPorRubroPublicoDto> volumenPorRubro) {
        this.volumenPorRubro = volumenPorRubro;
    }
}
