package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ViajeHistoricoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idViaje;
    private LocalDateTime fechaFin;
    private Long idEmpresa;
    private Long idVehiculo;
    private Long idRubro;
    private String rubro;
    private Long idTipoCarga;
    private int volumen;

    public ViajeHistoricoDto() {
    }

    public ViajeHistoricoDto(Long idViaje, LocalDateTime fechaFin, Long idEmpresa, Long idVehiculo, Long idRubro, String rubro,
            Long idTipoCarga, int volumen) {
        this.idViaje = idViaje;
        this.fechaFin = fechaFin;
        this.idEmpresa = idEmpresa;
        this.idVehiculo = idVehiculo;
        this.idRubro = idRubro;
        this.rubro = rubro;
        this.idTipoCarga = idTipoCarga;
        this.volumen = volumen;
    }

    public Long getIdViaje() {
        return idViaje;
    }

    public void setIdViaje(Long idViaje) {
        this.idViaje = idViaje;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public Long getIdVehiculo() {
        return idVehiculo;
    }

    public void setIdVehiculo(Long idVehiculo) {
        this.idVehiculo = idVehiculo;
    }

    public Long getIdRubro() {
        return idRubro;
    }

    public void setIdRubro(Long idRubro) {
        this.idRubro = idRubro;
    }

    public String getRubro() {
        return rubro;
    }

    public void setRubro(String rubro) {
        this.rubro = rubro;
    }

    public Long getIdTipoCarga() {
        return idTipoCarga;
    }

    public void setIdTipoCarga(Long idTipoCarga) {
        this.idTipoCarga = idTipoCarga;
    }

    public int getVolumen() {
        return volumen;
    }

    public void setVolumen(int volumen) {
        this.volumen = volumen;
    }
}
