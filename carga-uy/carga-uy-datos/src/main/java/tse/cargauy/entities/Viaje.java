package tse.cargauy.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;

// si un viaje del vehiculo quedo sin fin registrado y empezo otro, una posicion es del que empezo ultimo
@Entity
@NamedQueries({
    @NamedQuery(name = Viaje.EN_CURSO_DEL_VEHICULO,
            query = "SELECT v FROM Viaje v WHERE v.vehiculo.id = :idVehiculo AND v.fechaInicio <= :momento"
                    + " AND (v.fechaFin IS NULL OR v.fechaFin >= :momento) ORDER BY v.fechaInicio DESC"),
    @NamedQuery(name = Viaje.INICIADOS_DEL_VEHICULO,
            query = "SELECT v FROM Viaje v WHERE v.vehiculo.id = :idVehiculo AND v.fechaInicio IS NOT NULL"
                    + " AND (v.fechaFin IS NULL OR v.fechaFin >= :desde) ORDER BY v.fechaInicio")
})
public class Viaje {

    public static final String EN_CURSO_DEL_VEHICULO = "Viaje.enCursoDelVehiculo";
    public static final String INICIADOS_DEL_VEHICULO = "Viaje.iniciadosDelVehiculo";

    @Id @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoViaje estado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    private Guia guia;

    @ManyToOne(optional = false)
    private Vehiculo vehiculo;

    @ManyToOne(optional = false)
    private Chofer chofer;

    // si alguna balanza no respondio, cuando volver a evaluar el exceso de carga; null si no queda nada pendiente
    private LocalDateTime proximaEvaluacionCarga;

    // la web y el sync del celular pueden cambiar el mismo viaje a la vez: la segunda en confirmar pierde.
    // el default es para las filas que ya existian cuando se agrego la columna (schema-generation en update)
    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long version;

    public Viaje() {
    }

    public Viaje(Guia guia, Vehiculo vehiculo, Chofer chofer) {
        this.guia = guia;
        this.vehiculo = vehiculo;
        this.chofer = chofer;
        this.estado = EstadoViaje.ASIGNADO;
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

    public Guia getGuia() {
        return guia;
    }

    public void setGuia(Guia guia) {
        this.guia = guia;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(Vehiculo vehiculo) {
        this.vehiculo = vehiculo;
    }

    public Chofer getChofer() {
        return chofer;
    }

    public void setChofer(Chofer chofer) {
        this.chofer = chofer;
    }

    public LocalDateTime getProximaEvaluacionCarga() {
        return proximaEvaluacionCarga;
    }

    public void setProximaEvaluacionCarga(LocalDateTime proximaEvaluacionCarga) {
        this.proximaEvaluacionCarga = proximaEvaluacionCarga;
    }

}
