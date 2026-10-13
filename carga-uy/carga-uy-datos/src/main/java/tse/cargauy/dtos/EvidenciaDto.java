package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;

import tse.cargauy.entities.TipoEvidencia;

public class EvidenciaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private TipoEvidencia tipo;
    private String idOrigen;
    private LocalDateTime fechaEvento;
    private String valorObservado;
    private String valorDeclarado;
    private String umbral;
    private String descripcion;

    public EvidenciaDto() {
    }

    public EvidenciaDto(TipoEvidencia tipo, String idOrigen, LocalDateTime fechaEvento, String valorObservado, String valorDeclarado,
            String umbral, String descripcion) {
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
