package tse.cargauy.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// copia de un pesaje que devolvio un nodo de balanzas. el peaje y la balanza se guardan como vinieron:
// son datos del operador y la evidencia tiene que mostrar donde se peso aunque despues cambien en el nodo
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_pesaje_origen", columnNames = {"idOrigen"}),
        indexes = @Index(name = "ix_pesaje_vehiculo_fecha", columnList = "idVehiculo, fechaHora"))
public class Pesaje {
    @Id @GeneratedValue private Long id;
    @Column(nullable = false) private String idOrigen;      // nodo/balanza/idRegistro: el mismo pesaje consultado dos veces no se duplica
    @Column(nullable = false) private Long idNodo;
    @Column(nullable = false) private Long idVehiculo;
    private Long idViaje;                                    // null si no corresponde a ningun viaje
    @Column(nullable = false) private String matricula;
    @Column(nullable = false) private LocalDateTime fechaHora;
    private int pesoRegistrado;
    @Column(nullable = false) private String codigoBalanza;
    private String sentido;
    @Column(nullable = false) private String codigoPeaje;
    private String nombrePeaje;
    private String ruta;
    private int km;
    private double latitud;
    private double longitud;
    @Column(nullable = false) private LocalDateTime recibidoEn;

    public Pesaje() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdOrigen() {
        return idOrigen;
    }

    public void setIdOrigen(String idOrigen) {
        this.idOrigen = idOrigen;
    }

    public Long getIdNodo() {
        return idNodo;
    }

    public void setIdNodo(Long idNodo) {
        this.idNodo = idNodo;
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

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public int getPesoRegistrado() {
        return pesoRegistrado;
    }

    public void setPesoRegistrado(int pesoRegistrado) {
        this.pesoRegistrado = pesoRegistrado;
    }

    public String getCodigoBalanza() {
        return codigoBalanza;
    }

    public void setCodigoBalanza(String codigoBalanza) {
        this.codigoBalanza = codigoBalanza;
    }

    public String getSentido() {
        return sentido;
    }

    public void setSentido(String sentido) {
        this.sentido = sentido;
    }

    public String getCodigoPeaje() {
        return codigoPeaje;
    }

    public void setCodigoPeaje(String codigoPeaje) {
        this.codigoPeaje = codigoPeaje;
    }

    public String getNombrePeaje() {
        return nombrePeaje;
    }

    public void setNombrePeaje(String nombrePeaje) {
        this.nombrePeaje = nombrePeaje;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public int getKm() {
        return km;
    }

    public void setKm(int km) {
        this.km = km;
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

    public LocalDateTime getRecibidoEn() {
        return recibidoEn;
    }

    public void setRecibidoEn(LocalDateTime recibidoEn) {
        this.recibidoEn = recibidoEn;
    }
}
