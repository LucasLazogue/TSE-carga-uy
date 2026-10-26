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
@Table(indexes = @Index(name = "ix_informe_solicitante_fecha", columnList = "solicitante_id, fechaSolicitud"))
public class Informe {

    @Id @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private LocalDate periodoDesde;
    @Column(nullable = false)
    private LocalDate periodoHasta;

    @ManyToOne
    private TipoCarga tipoCarga;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormatoInforme formato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoInforme estado;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaGeneracion;

    private byte[] contenido;

    @ManyToOne(optional = false)
    private UsuarioBackoffice solicitante;

    public Informe() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getPeriodoDesde() {
        return periodoDesde;
    }

    public void setPeriodoDesde(LocalDate periodoDesde) {
        this.periodoDesde = periodoDesde;
    }

    public LocalDate getPeriodoHasta() {
        return periodoHasta;
    }

    public void setPeriodoHasta(LocalDate periodoHasta) {
        this.periodoHasta = periodoHasta;
    }

    public TipoCarga getTipoCarga() {
        return tipoCarga;
    }

    public void setTipoCarga(TipoCarga tipoCarga) {
        this.tipoCarga = tipoCarga;
    }

    public FormatoInforme getFormato() {
        return formato;
    }

    public void setFormato(FormatoInforme formato) {
        this.formato = formato;
    }

    public EstadoInforme getEstado() {
        return estado;
    }

    public void setEstado(EstadoInforme estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDateTime fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public byte[] getContenido() {
        return contenido;
    }

    public void setContenido(byte[] contenido) {
        this.contenido = contenido;
    }

    public UsuarioBackoffice getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(UsuarioBackoffice solicitante) {
        this.solicitante = solicitante;
    }
}
