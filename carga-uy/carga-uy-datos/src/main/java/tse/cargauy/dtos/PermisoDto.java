package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import tse.cargauy.entities.EstadoPermiso;

public class PermisoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nroPermiso;
    private LocalDate validoDesde;
    private LocalDate validoHasta;
    private Long idVehiculo;
    private String matricula;
    private EstadoPermiso estado;
    private String motivoRechazo;
    private String cedulaRevisor;
    private LocalDateTime fechaRevision;

    public PermisoDto() {
    }

    public PermisoDto(String nroPermiso, LocalDate validoDesde, LocalDate validoHasta, Long idVehiculo,
            EstadoPermiso estado) {
        this.nroPermiso = nroPermiso;
        this.validoDesde = validoDesde;
        this.validoHasta = validoHasta;
        this.idVehiculo = idVehiculo;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNroPermiso() {
        return nroPermiso;
    }

    public void setNroPermiso(String nroPermiso) {
        this.nroPermiso = nroPermiso;
    }

    public LocalDate getValidoDesde() {
        return validoDesde;
    }

    public void setValidoDesde(LocalDate validoDesde) {
        this.validoDesde = validoDesde;
    }

    public LocalDate getValidoHasta() {
        return validoHasta;
    }

    public void setValidoHasta(LocalDate validoHasta) {
        this.validoHasta = validoHasta;
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

    public EstadoPermiso getEstado() {
        return estado;
    }

    public void setEstado(EstadoPermiso estado) {
        this.estado = estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public String getCedulaRevisor() {
        return cedulaRevisor;
    }

    public void setCedulaRevisor(String cedulaRevisor) {
        this.cedulaRevisor = cedulaRevisor;
    }

    public LocalDateTime getFechaRevision() {
        return fechaRevision;
    }

    public void setFechaRevision(LocalDateTime fechaRevision) {
        this.fechaRevision = fechaRevision;
    }

    @Override
    public String toString() {
        return "Nro. Permiso: " + nroPermiso + ", Valido Desde: " + validoDesde +
                ", Valido Hasta: " + validoHasta + ", Vehiculo: " + matricula + ", Estado: " + estado;
    }
}
