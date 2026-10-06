package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.Instant;

public class EventoPosicionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String idEvento;
    private String nodo;
    private Long idNodo;
    private String matricula;
    private Long idVehiculo;
    private double latitud;
    private double longitud;
    private Instant timestampGeneracion;
    private Instant timestampRecepcion;

    public EventoPosicionDto() {
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

    public String getNodo() {
        return nodo;
    }

    public void setNodo(String nodo) {
        this.nodo = nodo;
    }

    public Long getIdNodo() {
        return idNodo;
    }

    public void setIdNodo(Long idNodo) {
        this.idNodo = idNodo;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public Long getIdVehiculo() {
        return idVehiculo;
    }

    public void setIdVehiculo(Long idVehiculo) {
        this.idVehiculo = idVehiculo;
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
