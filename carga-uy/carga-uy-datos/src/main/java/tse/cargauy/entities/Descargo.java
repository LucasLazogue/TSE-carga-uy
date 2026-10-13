package tse.cargauy.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class Descargo {

    @Id @GeneratedValue
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    private CasoFiscalizacion caso;

    @Column(columnDefinition = "text", nullable = false)
    private String texto;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(optional = false)
    private Ciudadano presentadoPor;

    public Descargo() {
    }

    public Descargo(CasoFiscalizacion caso, String texto, LocalDateTime fecha, Ciudadano presentadoPor) {
        this.caso = caso;
        this.texto = texto;
        this.fecha = fecha;
        this.presentadoPor = presentadoPor;
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

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Ciudadano getPresentadoPor() {
        return presentadoPor;
    }

    public void setPresentadoPor(Ciudadano presentadoPor) {
        this.presentadoPor = presentadoPor;
    }

}
