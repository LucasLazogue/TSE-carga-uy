package tse.cargauy.nodobalanzas.datos;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(indexes = {
    @Index(name = "ix_pesaje_matricula_fecha", columnList = "matricula, fechaHora"),
    @Index(name = "ix_pesaje_rut_fecha", columnList = "rutEmpresa, fechaHora")
})
public class Pesaje {

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
