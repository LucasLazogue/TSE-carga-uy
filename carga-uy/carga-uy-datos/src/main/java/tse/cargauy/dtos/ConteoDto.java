package tse.cargauy.dtos;

import java.io.Serializable;

public class ConteoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String etiqueta;
    private long valor;

    public ConteoDto() {
    }

    public ConteoDto(String etiqueta, long valor) {
        this.etiqueta = etiqueta;
        this.valor = valor;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public void setEtiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public long getValor() {
        return valor;
    }

    public void setValor(long valor) {
        this.valor = valor;
    }
}
