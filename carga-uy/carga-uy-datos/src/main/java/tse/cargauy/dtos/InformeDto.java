package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import tse.cargauy.entities.EstadoInforme;
import tse.cargauy.entities.FormatoInforme;

public class InformeDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private LocalDate periodoDesde;
    private LocalDate periodoHasta;
    private Long idTipoCarga;
    private String tipoCarga;
    private FormatoInforme formato;
    private EstadoInforme estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaGeneracion;
    private Long idSolicitante;

    public InformeDto() {
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

    public Long getIdTipoCarga() {
        return idTipoCarga;
    }

    public void setIdTipoCarga(Long idTipoCarga) {
        this.idTipoCarga = idTipoCarga;
    }

    public String getTipoCarga() {
        return tipoCarga;
    }

    public void setTipoCarga(String tipoCarga) {
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

    public Long getIdSolicitante() {
        return idSolicitante;
    }

    public void setIdSolicitante(Long idSolicitante) {
        this.idSolicitante = idSolicitante;
    }
}
