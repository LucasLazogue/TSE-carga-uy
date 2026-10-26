package tse.cargauy.nodobalanzas.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

@Entity
@NamedQueries({
    @NamedQuery(name = Balanza.POR_CODIGO,
            query = "SELECT b FROM Balanza b WHERE b.codigo = :codigo"),
    @NamedQuery(name = Balanza.POR_CLAVE,
            query = "SELECT b FROM Balanza b WHERE b.claveHash = :claveHash")
})
public class Balanza {

    public static final String POR_CODIGO = "Balanza.porCodigo";
    public static final String POR_CLAVE = "Balanza.porClave";

    @Id @GeneratedValue
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "peaje_id")
    private Peaje peaje;
    @Column(unique = true, nullable = false)
    private String codigo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sentido sentido;
    @Column(unique = true, nullable = false)
    private String claveHash;

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

    public String getClaveHash() {
        return claveHash;
    }

    public void setClaveHash(String claveHash) {
        this.claveHash = claveHash;
    }
}
