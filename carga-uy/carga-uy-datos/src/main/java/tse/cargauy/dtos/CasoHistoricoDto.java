package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;

import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.TipoInfraccion;

public class CasoHistoricoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idCaso;
    private TipoInfraccion tipo;
    private EstadoCaso estado;
    private LocalDateTime fechaDeteccion;
    private Long idTipoCarga;

    public CasoHistoricoDto() {
    }

    public CasoHistoricoDto(Long idCaso, TipoInfraccion tipo, EstadoCaso estado, LocalDateTime fechaDeteccion, Long idTipoCarga) {
        this.idCaso = idCaso;
        this.tipo = tipo;
        this.estado = estado;
        this.fechaDeteccion = fechaDeteccion;
        this.idTipoCarga = idTipoCarga;
    }

    public Long getIdCaso() {
        return idCaso;
    }

    public void setIdCaso(Long idCaso) {
        this.idCaso = idCaso;
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

    public LocalDateTime getFechaDeteccion() {
        return fechaDeteccion;
    }

    public void setFechaDeteccion(LocalDateTime fechaDeteccion) {
        this.fechaDeteccion = fechaDeteccion;
    }

    public Long getIdTipoCarga() {
        return idTipoCarga;
    }

    public void setIdTipoCarga(Long idTipoCarga) {
        this.idTipoCarga = idTipoCarga;
    }
}
