package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;

public class NormaFiscalizacionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String clave;
    private int valor;
    private LocalDate vigenteDesde;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public int getValor() {
        return valor;
    }

    public void setValor(int valor) {
        this.valor = valor;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public void setVigenteDesde(LocalDate vigenteDesde) {
        this.vigenteDesde = vigenteDesde;
    }
}
