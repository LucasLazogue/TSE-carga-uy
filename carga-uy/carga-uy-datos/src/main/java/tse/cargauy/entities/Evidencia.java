package tse.cargauy.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_evidencia_origen", columnNames = {"caso_id", "tipo", "idOrigen"}))
public class Evidencia {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    private CasoFiscalizacion caso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEvidencia tipo;

    @Column(nullable = false)
    private String idOrigen;

    private LocalDateTime fechaEvento;
    private String valorObservado;
    private String valorDeclarado;
    private String umbral;
    @Column(columnDefinition = "text")
    private String descripcion;

    public Evidencia() {
    }

    public Evidencia(CasoFiscalizacion caso, TipoEvidencia tipo, String idOrigen, LocalDateTime fechaEvento,
            String valorObservado, String valorDeclarado, String umbral, String descripcion) {
        this.caso = caso;
        this.tipo = tipo;
        this.idOrigen = idOrigen;
        this.fechaEvento = fechaEvento;
        this.valorObservado = valorObservado;
        this.valorDeclarado = valorDeclarado;
        this.umbral = umbral;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CasoFiscalizacion getCaso() {
        return caso;
    }

    public void setCaso(CasoFiscalizacion caso) {
        this.caso = caso;
    }

    public TipoEvidencia getTipo() {
        return tipo;
    }

    public void setTipo(TipoEvidencia tipo) {
        this.tipo = tipo;
    }

    public String getIdOrigen() {
        return idOrigen;
    }

    public void setIdOrigen(String idOrigen) {
        this.idOrigen = idOrigen;
    }

    public LocalDateTime getFechaEvento() {
        return fechaEvento;
    }

    public void setFechaEvento(LocalDateTime fechaEvento) {
        this.fechaEvento = fechaEvento;
    }

    public String getValorObservado() {
        return valorObservado;
    }

    public void setValorObservado(String valorObservado) {
        this.valorObservado = valorObservado;
    }

    public String getValorDeclarado() {
        return valorDeclarado;
    }

    public void setValorDeclarado(String valorDeclarado) {
        this.valorDeclarado = valorDeclarado;
    }

    public String getUmbral() {
        return umbral;
    }

    public void setUmbral(String umbral) {
        this.umbral = umbral;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

}
