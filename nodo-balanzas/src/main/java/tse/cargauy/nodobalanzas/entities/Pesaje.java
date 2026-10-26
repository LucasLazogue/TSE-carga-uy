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
import jakarta.persistence.UniqueConstraint;

// el indice es para que la consulta del central sea rapida aunque haya millones de pesajes.
// el central pregunta "pesajes de la matricula SCC9012 el 2026-10-20".
// sin indice postgres tendria que recorrer toda la tabla pesaje fila por fila para encontrarlos.
// con el indice (matricula, fechaHora) los tiene ordenados como en una guia telefonica: primero por
// matricula y adentro por fecha, asi salta directo a SCC9012 y lee solo las filas de ese dia.
//
// la restriccion unica (balanza, idRegistro) es para los reenvios: cada lectura trae un numero que le pone
// la balanza (ej. balanza-r5-km30 lectura 1001). si se corta la conexion y la balanza vuelve a mandar
// la lectura 1001, ya existe y no se guarda dos veces
@Entity
@Table(indexes = @Index(name = "ix_pesaje_matricula_fecha", columnList = "matricula, fechaHora"),
        uniqueConstraints = @UniqueConstraint(name = "uk_pesaje_balanza_registro", columnNames = {"balanza_id", "idRegistro"}))
@NamedQueries({
    @NamedQuery(name = Pesaje.POR_MATRICULA,
            query = "SELECT p FROM Pesaje p WHERE p.matricula = :matricula"
                    + " AND p.fechaHora >= :desde AND p.fechaHora < :hasta ORDER BY p.fechaHora"),
    @NamedQuery(name = Pesaje.CANTIDAD_POR_REGISTRO,
            query = "SELECT COUNT(p) FROM Pesaje p WHERE p.balanza.codigo = :balanza AND p.idRegistro = :idRegistro")
})
public class Pesaje {

    public static final String POR_MATRICULA = "Pesaje.porMatricula";
    public static final String CANTIDAD_POR_REGISTRO = "Pesaje.cantidadPorRegistro";

    @Id @GeneratedValue
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "balanza_id")
    private Balanza balanza;
    @Column(nullable = false)
    private String idRegistro;
    @Column(nullable = false)
    private String matricula;
    @Column(nullable = false)
    private LocalDateTime fechaHora;
    private int pesoRegistrado;
    @Column(nullable = false)
    private LocalDateTime recibidoEn;

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

    public String getIdRegistro() {
        return idRegistro;
    }

    public void setIdRegistro(String idRegistro) {
        this.idRegistro = idRegistro;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
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

    public LocalDateTime getRecibidoEn() {
        return recibidoEn;
    }

    public void setRecibidoEn(LocalDateTime recibidoEn) {
        this.recibidoEn = recibidoEn;
    }
}
