package tse.cargauy.nodobalanzas.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

// los indices son para que la consulta del central sea rapida aunque haya millones de pesajes.
// el central pregunta "pesajes de la matricula SCC9012 el 2026-10-20" (o lo mismo por rut de empresa).
// sin indice postgres tendria que recorrer toda la tabla pesaje fila por fila para encontrarlos.
// con el indice (matricula, fechaHora) los tiene ordenados como en una guia telefonica: primero por
// matricula y adentro por fecha, asi salta directo a SCC9012 y lee solo las filas de ese dia.
// el segundo indice hace lo mismo cuando la consulta viene por rut en vez de matricula
@Entity
@Table(indexes = {
    @Index(name = "ix_pesaje_matricula_fecha", columnList = "matricula, fechaHora"),
    @Index(name = "ix_pesaje_rut_fecha", columnList = "rutEmpresa, fechaHora")
})
@NamedQueries({
    @NamedQuery(name = Pesaje.POR_MATRICULA,
            query = "SELECT p FROM Pesaje p WHERE p.matricula = :matricula"
                    + " AND p.fechaHora >= :desde AND p.fechaHora < :hasta ORDER BY p.fechaHora"),
    @NamedQuery(name = Pesaje.POR_RUT,
            query = "SELECT p FROM Pesaje p WHERE p.rutEmpresa = :rut"
                    + " AND p.fechaHora >= :desde AND p.fechaHora < :hasta ORDER BY p.fechaHora")
})
public class Pesaje {

    public static final String POR_MATRICULA = "Pesaje.porMatricula";
    public static final String POR_RUT = "Pesaje.porRut";

    @Id @GeneratedValue
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "balanza_id")
    private Balanza balanza;
    @Column(nullable = false)
    private String matricula;
    private String rutEmpresa;
    @Column(nullable = false)
    private LocalDateTime fechaHora;
    private int pesoRegistrado;

    public Pesaje() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Balanza getBalanza() {
        return balanza;
    }

    public void setBalanza(Balanza balanza) {
        this.balanza = balanza;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getRutEmpresa() {
        return rutEmpresa;
    }

    public void setRutEmpresa(String rutEmpresa) {
        this.rutEmpresa = rutEmpresa;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public int getPesoRegistrado() {
        return pesoRegistrado;
    }

    public void setPesoRegistrado(int pesoRegistrado) {
        this.pesoRegistrado = pesoRegistrado;
    }
}
