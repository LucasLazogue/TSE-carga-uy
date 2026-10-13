package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.Instant;

public class EventoPosicionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    // el _id que le asigna MongoDB
    private String id;
    private String idEvento;
    private String nodo;
    private Long idNodo;
    private String matricula;
    private Long idVehiculo;
    // el viaje que el vehiculo tenia en curso cuando se genero la posicion; null si no tenia
    private Long idViaje;
    private double latitud;
    private double longitud;
    private Instant timestampGeneracion;
    private Instant timestampRecepcion;
    // true recien cuando se confirmo la transaccion que la proceso: si quedo en false, un reenvio la termina (DA07)
    private boolean procesada;

    public EventoPosicionDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public Long getIdViaje() {
        return idViaje;
    }

    public void setIdViaje(Long idViaje) {
        this.idViaje = idViaje;
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

    public boolean isProcesada() {
        return procesada;
    }

    public void setProcesada(boolean procesada) {
        this.procesada = procesada;
    }
}
