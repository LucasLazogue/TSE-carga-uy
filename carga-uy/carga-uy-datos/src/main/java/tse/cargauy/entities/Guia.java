package tse.cargauy.entities;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class Guia {

    @Id @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false)
    private String nroGuia;
    private LocalDate fecha;
    private String origen;
    private String destino;
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

    public Guia(String nroGuia, LocalDate fecha, String origen, String destino, int volumen,
                Empresa empresa, Ciudadano registradaPor, Rubro rubro, TipoCarga tipoCarga) {
        this.nroGuia = nroGuia;
        this.fecha = fecha;
        this.origen = origen;
        this.destino = destino;
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
        return nroGuia;
    }

    public void setNroGuia(String nroGuia) {
        this.nroGuia = nroGuia;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
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
