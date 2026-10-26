package tse.cargauy.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(indexes = @Index(name = "ix_permiso_estado_vigencia", columnList = "estado, validoDesde, validoHasta"))
public class Permiso {

    @Id @GeneratedValue
    private Long id;
    @Column(unique = true, nullable = false)
    private String nroPermiso;
    private LocalDate validoDesde;
    private LocalDate validoHasta;
    @ManyToOne(optional = false)
    private Vehiculo vehiculo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'APROBADO' not null")
    private EstadoPermiso estado;
    @Column(columnDefinition = "text")
    private String motivoRechazo;
    @ManyToOne
    private Funcionario revisor;
    private LocalDateTime fechaRevision;

    public Permiso() {
    }

    public Permiso(String nroPermiso, LocalDate validoDesde, LocalDate validoHasta, Vehiculo vehiculo) {
        this.nroPermiso = nroPermiso;
        this.validoDesde = validoDesde;
        this.validoHasta = validoHasta;
        this.vehiculo = vehiculo;
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

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(Vehiculo vehiculo) {
        this.vehiculo = vehiculo;
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

    public Funcionario getRevisor() {
        return revisor;
    }

    public void setRevisor(Funcionario revisor) {
        this.revisor = revisor;
    }

    public LocalDateTime getFechaRevision() {
        return fechaRevision;
    }

    public void setFechaRevision(LocalDateTime fechaRevision) {
        this.fechaRevision = fechaRevision;
    }

}
