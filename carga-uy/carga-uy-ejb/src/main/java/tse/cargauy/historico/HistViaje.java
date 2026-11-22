package tse.cargauy.historico;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import tse.cargauy.dtos.ViajeHistoricoDto;

@Entity
@Table(name = "hist_viaje", indexes = @Index(name = "ix_hist_viaje_fin", columnList = "fechaFin"))
public class HistViaje {

    @Id
    private Long idViaje;

    @Column(nullable = false)
    private LocalDateTime fechaFin;

    @Column(nullable = false)
    private Long idEmpresa;

    @Column(nullable = false)
    private Long idVehiculo;

    @Column(nullable = false)
    private Long idRubro;

    @Column(nullable = false)
    private String rubro;

    @Column(nullable = false)
    private Long idTipoCarga;

    @Column(nullable = false)
    private int volumen;

    public HistViaje() {
    }

    public HistViaje(ViajeHistoricoDto viaje) {
        this.idViaje = viaje.getIdViaje();
        this.fechaFin = viaje.getFechaFin();
        this.idEmpresa = viaje.getIdEmpresa();
        this.idVehiculo = viaje.getIdVehiculo();
        this.idRubro = viaje.getIdRubro();
        this.rubro = viaje.getRubro();
        this.idTipoCarga = viaje.getIdTipoCarga();
        this.volumen = viaje.getVolumen();
    }

    public Long getIdViaje() {
        return idViaje;
    }
}
