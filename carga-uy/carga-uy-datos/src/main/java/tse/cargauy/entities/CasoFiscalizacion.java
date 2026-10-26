package tse.cargauy.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(indexes = @Index(name = "ix_caso_deteccion", columnList = "fechaDeteccion"))
public class CasoFiscalizacion {

    @Id @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoInfraccion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCaso estado;

    // identifica el hecho: reprocesar el mismo evento no crea otro caso
    @Column(unique = true, nullable = false)
    private String claveDeduplicacion;

    @Column(nullable = false)
    private LocalDateTime fechaHecho;
    @Column(nullable = false)
    private LocalDateTime fechaDeteccion;
    private LocalDateTime fechaNotificacion;
    private LocalDateTime venceDescargo;

    @ManyToOne(optional = false)
    private Vehiculo vehiculo;

    @ManyToOne(optional = false)
    private Empresa empresa;

    @ManyToOne
    private Viaje viaje;

    @OneToMany(mappedBy = "caso")
    @OrderBy("id")
    private List<Evidencia> evidencias = new ArrayList<>();

    @OneToOne(mappedBy = "caso")
    private Descargo descargo;

    @OneToOne(mappedBy = "caso")
    private Resolucion resolucion;

    @Version
    private long version;

    public CasoFiscalizacion() {
    }

    public CasoFiscalizacion(TipoInfraccion tipo, String claveDeduplicacion, LocalDateTime fechaHecho, LocalDateTime fechaDeteccion,
            Vehiculo vehiculo, Viaje viaje) {
        this.tipo = tipo;
        this.estado = EstadoCaso.DETECTADA;
        this.claveDeduplicacion = claveDeduplicacion;
        this.fechaHecho = fechaHecho;
        this.fechaDeteccion = fechaDeteccion;
        this.vehiculo = vehiculo;
        this.empresa = vehiculo.getEmpresa();
        this.viaje = viaje;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoInfraccion getTipo() {
        return tipo;
    }

    public void setTipo(TipoInfraccion tipo) {
        this.tipo = tipo;
    }

    public EstadoCaso getEstado() {
        return estado;
    }

    public void setEstado(EstadoCaso estado) {
        this.estado = estado;
    }

    public String getClaveDeduplicacion() {
        return claveDeduplicacion;
    }

    public void setClaveDeduplicacion(String claveDeduplicacion) {
        this.claveDeduplicacion = claveDeduplicacion;
    }

    public LocalDateTime getFechaHecho() {
        return fechaHecho;
    }

    public void setFechaHecho(LocalDateTime fechaHecho) {
        this.fechaHecho = fechaHecho;
    }

    public LocalDateTime getFechaDeteccion() {
        return fechaDeteccion;
    }

    public void setFechaDeteccion(LocalDateTime fechaDeteccion) {
        this.fechaDeteccion = fechaDeteccion;
    }

    public LocalDateTime getFechaNotificacion() {
        return fechaNotificacion;
    }

    public void setFechaNotificacion(LocalDateTime fechaNotificacion) {
        this.fechaNotificacion = fechaNotificacion;
    }

    public LocalDateTime getVenceDescargo() {
        return venceDescargo;
    }

    public void setVenceDescargo(LocalDateTime venceDescargo) {
        this.venceDescargo = venceDescargo;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(Vehiculo vehiculo) {
        this.vehiculo = vehiculo;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Viaje getViaje() {
        return viaje;
    }

    public void setViaje(Viaje viaje) {
        this.viaje = viaje;
    }

    public List<Evidencia> getEvidencias() {
        return evidencias;
    }

    public void setEvidencias(List<Evidencia> evidencias) {
        this.evidencias = evidencias;
    }

    public Descargo getDescargo() {
        return descargo;
    }

    public void setDescargo(Descargo descargo) {
        this.descargo = descargo;
    }

    public Resolucion getResolucion() {
        return resolucion;
    }

    public void setResolucion(Resolucion resolucion) {
        this.resolucion = resolucion;
    }

    public long getVersion() {
        return version;
    }

}
