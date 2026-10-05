package tse.cargauy.entities;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class Guia {

    @Id @GeneratedValue
    private Long id;

    private LocalDate fecha;
    private double origenLat;
    private double origenLon;
    private double destinoLat;
    private double destinoLon;
    private int volumen;

    @ManyToOne(optional = false)
    private Empresa empresa;

    @ManyToOne(optional = false)
    private Ciudadano registradaPor;

    @ManyToOne(optional = false)
    private Rubro rubro;

    @ManyToOne(optional = false)
    private TipoCarga tipoCarga;

    @OneToOne(mappedBy = "guia")
    private Viaje viaje;

    public Guia() {
    }

    public Guia(LocalDate fecha, double origenLat, double origenLon, double destinoLat, double destinoLon, int volumen,
                Empresa empresa, Ciudadano registradaPor, Rubro rubro, TipoCarga tipoCarga) {
        this.fecha = fecha;
        this.origenLat = origenLat;
        this.origenLon = origenLon;
        this.destinoLat = destinoLat;
        this.destinoLon = destinoLon;
        this.volumen = volumen;
        this.empresa = empresa;
        this.registradaPor = registradaPor;
        this.rubro = rubro;
        this.tipoCarga = tipoCarga;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNroGuia() {
        return String.format("G-%06d", id);
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public double getOrigenLat() {
        return origenLat;
    }

    public void setOrigenLat(double origenLat) {
        this.origenLat = origenLat;
    }

    public double getOrigenLon() {
        return origenLon;
    }

    public void setOrigenLon(double origenLon) {
        this.origenLon = origenLon;
    }

    public double getDestinoLat() {
        return destinoLat;
    }

    public void setDestinoLat(double destinoLat) {
        this.destinoLat = destinoLat;
    }

    public double getDestinoLon() {
        return destinoLon;
    }

    public void setDestinoLon(double destinoLon) {
        this.destinoLon = destinoLon;
    }

    public int getVolumen() {
        return volumen;
    }

    public void setVolumen(int volumen) {
        this.volumen = volumen;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Ciudadano getRegistradaPor() {
        return registradaPor;
    }

    public void setRegistradaPor(Ciudadano registradaPor) {
        this.registradaPor = registradaPor;
    }

    public Rubro getRubro() {
        return rubro;
    }

    public void setRubro(Rubro rubro) {
        this.rubro = rubro;
    }

    public TipoCarga getTipoCarga() {
        return tipoCarga;
    }

    public void setTipoCarga(TipoCarga tipoCarga) {
        this.tipoCarga = tipoCarga;
    }

    public Viaje getViaje() {
        return viaje;
    }

    public void setViaje(Viaje viaje) {
        this.viaje = viaje;
    }

}
