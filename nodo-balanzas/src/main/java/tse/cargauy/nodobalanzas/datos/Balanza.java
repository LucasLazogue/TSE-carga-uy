package tse.cargauy.nodobalanzas.datos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_balanza_peaje_codigo", columnNames = {"peaje_id", "codigo"}))
public class Balanza {

    @Id @GeneratedValue
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "peaje_id")
    private Peaje peaje;
    @Column(nullable = false)
    private String codigo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sentido sentido;

    public Balanza() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Peaje getPeaje() {
        return peaje;
    }

    public void setPeaje(Peaje peaje) {
        this.peaje = peaje;
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
}
