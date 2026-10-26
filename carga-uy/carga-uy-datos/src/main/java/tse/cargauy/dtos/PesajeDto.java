package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PesajeDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String idOrigen;
    private Long idNodo;
    private Long idVehiculo;
    private Long idViaje;
    private String matricula;
    private LocalDateTime fechaHora;
    private int pesoRegistrado;
    private String codigoBalanza;
    private String sentido;
    private String codigoPeaje;
    private String nombrePeaje;
    private String ruta;
    private int km;
    private double latitud;
    private double longitud;
    private LocalDateTime recibidoEn;

    public PesajeDto() {
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
