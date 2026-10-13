package tse.cargauy.entities;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

// por ahora solo el plazo de descargo; se unifica con la parametrizacion de reglas cuando exista
@Entity
public class NormaFiscalizacion {

    public static final String PLAZO_DESCARGO_DIAS = "PLAZO_DESCARGO_DIAS";

    @Id @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String clave;

    private int valor;

    @Column(nullable = false)
    private LocalDate vigenteDesde;

    public NormaFiscalizacion() {
    }

    public NormaFiscalizacion(String clave, int valor, LocalDate vigenteDesde) {
        this.clave = clave;
        this.valor = valor;
        this.vigenteDesde = vigenteDesde;
    }

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
