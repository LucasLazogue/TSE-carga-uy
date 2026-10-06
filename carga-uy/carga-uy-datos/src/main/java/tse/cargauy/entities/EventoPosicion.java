package tse.cargauy.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// el mismo evento reenviado por el mismo nodo choca contra la restriccion unica: es lo que hace idempotente la ingesta
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_evento_posicion_origen", columnNames = {"nodo_id", "idEvento"}))
@NamedQueries({
    @NamedQuery(name = EventoPosicion.RUTA_POR_MATRICULA,
            query = "SELECT e FROM EventoPosicion e WHERE e.matricula = :matricula ORDER BY e.timestampGeneracion")
})
public class EventoPosicion {

    public static final String RUTA_POR_MATRICULA = "EventoPosicion.rutaPorMatricula";

    @Id @GeneratedValue
    private Long id;
    @Column(nullable = false)
    private String idEvento;
    @ManyToOne(optional = false)
    private NodoPeriferico nodo;
    @Column(nullable = false)
    private String matricula;
    // null si la matricula no corresponde a un vehiculo registrado
    @ManyToOne
    private Vehiculo vehiculo;
    private double latitud;
    private double longitud;
    @Column(nullable = false)
    private Instant timestampGeneracion;
    @Column(nullable = false)
    private Instant timestampRecepcion;

    public EventoPosicion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(String idEvento) {
        this.idEvento = idEvento;
    }

    public NodoPeriferico getNodo() {
        return nodo;
    }

    public void setNodo(NodoPeriferico nodo) {
        this.nodo = nodo;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(Vehiculo vehiculo) {
        this.vehiculo = vehiculo;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public Instant getTimestampGeneracion() {
        return timestampGeneracion;
    }

    public void setTimestampGeneracion(Instant timestampGeneracion) {
        this.timestampGeneracion = timestampGeneracion;
    }

    public Instant getTimestampRecepcion() {
        return timestampRecepcion;
    }

    public void setTimestampRecepcion(Instant timestampRecepcion) {
        this.timestampRecepcion = timestampRecepcion;
    }

}
