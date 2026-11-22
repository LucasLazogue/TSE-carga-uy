package tse.cargauy.historico;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import tse.cargauy.dtos.CasoHistoricoDto;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.TipoInfraccion;

@Entity
@Table(name = "hist_caso", indexes = @Index(name = "ix_hist_caso_deteccion", columnList = "fechaDeteccion"))
public class HistCaso {

    @Id
    private Long idCaso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoInfraccion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCaso estado;

    @Column(nullable = false)
    private LocalDateTime fechaDeteccion;

    @Column
    private Long idTipoCarga;

    public HistCaso() {
    }

    public HistCaso(CasoHistoricoDto caso) {
        this.idCaso = caso.getIdCaso();
        this.tipo = caso.getTipo();
        this.estado = caso.getEstado();
        this.fechaDeteccion = caso.getFechaDeteccion();
        this.idTipoCarga = caso.getIdTipoCarga();
    }

    public Long getIdCaso() {
        return idCaso;
    }

    public EstadoCaso getEstado() {
        return estado;
    }
}
