package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import tse.cargauy.entities.EstadoViaje;

public class ViajeDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private EstadoViaje estado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Long idGuia;
    private String nroGuia;
    private Long idVehiculo;
    private String matricula;
    private Long idChofer;
    private String cedulaChofer;

    public ViajeDto() {
    }

    public ViajeDto(Long idGuia, Long idVehiculo, Long idChofer) {
        this.idGuia = idGuia;
        this.idVehiculo = idVehiculo;
        this.idChofer = idChofer;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EstadoViaje getEstado() {
        return estado;
    }

    public void setEstado(EstadoViaje estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Long getIdGuia() {
        return idGuia;
    }

    public void setIdGuia(Long idGuia) {
        this.idGuia = idGuia;
    }

    public String getNroGuia() {
        return nroGuia;
    }

    public void setNroGuia(String nroGuia) {
        this.nroGuia = nroGuia;
    }

    public Long getIdVehiculo() {
        return idVehiculo;
    }

    public void setIdVehiculo(Long idVehiculo) {
        this.idVehiculo = idVehiculo;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public Long getIdChofer() {
        return idChofer;
    }

    public void setIdChofer(Long idChofer) {
        this.idChofer = idChofer;
    }

    public String getCedulaChofer() {
        return cedulaChofer;
    }

    public void setCedulaChofer(String cedulaChofer) {
        this.cedulaChofer = cedulaChofer;
    }

    @Override
    public String toString() {
        return "Guia: " + nroGuia + ", Vehiculo: " + matricula + ", Chofer: " + cedulaChofer + ", Estado: " + estado;
    }
}
