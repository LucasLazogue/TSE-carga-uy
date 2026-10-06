package tse.cargauy.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

@Entity
@NamedQueries({
    @NamedQuery(name = NodoPeriferico.VIGENTES,
            query = "SELECT n FROM NodoPeriferico n WHERE n.estado <> :baja ORDER BY n.identificador"),
    @NamedQuery(name = NodoPeriferico.POR_IDENTIFICADOR,
            query = "SELECT n FROM NodoPeriferico n WHERE n.identificador = :identificador")
})
public class NodoPeriferico {

    public static final String VIGENTES = "NodoPeriferico.vigentes";
    public static final String POR_IDENTIFICADOR = "NodoPeriferico.porIdentificador";

    @Id @GeneratedValue
    private Long id;
    @Column(unique = true, nullable = false)
    private String identificador;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNodo tipo;
    private String puntoAcceso;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoNodo estado;

    public NodoPeriferico() {
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
