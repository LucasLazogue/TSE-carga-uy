package tse.cargauy.dtos;

import java.io.Serializable;

import tse.cargauy.entities.EstadoNodo;
import tse.cargauy.entities.TipoNodo;

public class NodoPerifericoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String identificador;
    private TipoNodo tipo;
    private String puntoAcceso;
    private EstadoNodo estado;

    public NodoPerifericoDto() {
    }

    public NodoPerifericoDto(String identificador, TipoNodo tipo, String puntoAcceso) {
        this.identificador = identificador;
        this.tipo = tipo;
        this.puntoAcceso = puntoAcceso;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public TipoNodo getTipo() {
        return tipo;
    }

    public void setTipo(TipoNodo tipo) {
        this.tipo = tipo;
    }

    public String getPuntoAcceso() {
        return puntoAcceso;
    }

    public void setPuntoAcceso(String puntoAcceso) {
        this.puntoAcceso = puntoAcceso;
    }

    public EstadoNodo getEstado() {
        return estado;
    }

    public void setEstado(EstadoNodo estado) {
        this.estado = estado;
    }

}
