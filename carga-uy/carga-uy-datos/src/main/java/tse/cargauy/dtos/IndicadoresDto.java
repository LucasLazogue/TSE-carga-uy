package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class IndicadoresDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private FiltroInforme filtro;
    private boolean periodoAmplio;
    private LocalDate habilitadosAl;
    private long empresasHabilitadas;
    private long vehiculosHabilitados;
    private Granularidad granularidad;
    private List<ViajesPorPeriodoDto> viajes = new ArrayList<>();
    private List<VolumenPorRubroDto> volumenPorRubro = new ArrayList<>();
    private List<InfraccionesPorTipoDto> infracciones = new ArrayList<>();

    public IndicadoresDto() {
    }

    public long getDetectadas() {
        return infracciones.stream().mapToLong(InfraccionesPorTipoDto::getDetectadas).sum();
    }

    public long getConfirmadas() {
        return infracciones.stream().mapToLong(InfraccionesPorTipoDto::getConfirmadas).sum();
    }

    public long getPendientes() {
        return infracciones.stream().mapToLong(InfraccionesPorTipoDto::getPendientes).sum();
    }

    public Double getTasaConfirmadas() {
        long detectadas = getDetectadas();
        return detectadas == 0 ? null : (double) getConfirmadas() / detectadas;
    }

    public boolean isSinDatos() {
        return viajes.isEmpty() && getDetectadas() == 0;
    }

    public FiltroInforme getFiltro() {
        return filtro;
    }

    public void setFiltro(FiltroInforme filtro) {
        this.filtro = filtro;
    }

    public boolean isPeriodoAmplio() {
        return periodoAmplio;
    }

    public void setPeriodoAmplio(boolean periodoAmplio) {
        this.periodoAmplio = periodoAmplio;
    }

    public LocalDate getHabilitadosAl() {
        return habilitadosAl;
    }

    public void setHabilitadosAl(LocalDate habilitadosAl) {
        this.habilitadosAl = habilitadosAl;
    }

    public long getEmpresasHabilitadas() {
        return empresasHabilitadas;
    }

    public void setEmpresasHabilitadas(long empresasHabilitadas) {
        this.empresasHabilitadas = empresasHabilitadas;
    }

    public long getVehiculosHabilitados() {
        return vehiculosHabilitados;
    }

    public void setVehiculosHabilitados(long vehiculosHabilitados) {
        this.vehiculosHabilitados = vehiculosHabilitados;
    }

    public Granularidad getGranularidad() {
        return granularidad;
    }

    public void setGranularidad(Granularidad granularidad) {
        this.granularidad = granularidad;
    }

    public List<ViajesPorPeriodoDto> getViajes() {
        return viajes;
    }

    public void setViajes(List<ViajesPorPeriodoDto> viajes) {
        this.viajes = viajes;
    }

    public List<VolumenPorRubroDto> getVolumenPorRubro() {
        return volumenPorRubro;
    }

    public void setVolumenPorRubro(List<VolumenPorRubroDto> volumenPorRubro) {
        this.volumenPorRubro = volumenPorRubro;
    }

    public List<InfraccionesPorTipoDto> getInfracciones() {
        return infracciones;
    }

    public void setInfracciones(List<InfraccionesPorTipoDto> infracciones) {
        this.infracciones = infracciones;
    }
}
