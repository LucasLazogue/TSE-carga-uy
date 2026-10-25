package tse.cargauy.nodobalanzas.dtos;

import java.io.Serializable;

import tse.cargauy.nodobalanzas.entities.Sentido;

public class BalanzaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String codigo;
    private Sentido sentido;
    private String clave;

    public BalanzaDto() {
    }

    public BalanzaDto(String codigo, Sentido sentido, String clave) {
        this.codigo = codigo;
        this.sentido = sentido;
        this.clave = clave;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Sentido getSentido() {
        return sentido;
    }

    public void setSentido(Sentido sentido) {
        this.sentido = sentido;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }
}
