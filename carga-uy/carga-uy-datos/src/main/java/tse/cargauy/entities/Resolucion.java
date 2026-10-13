package tse.cargauy.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class Resolucion {

    @Id @GeneratedValue
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    private CasoFiscalizacion caso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCaso resultado;

    @Column(columnDefinition = "text", nullable = false)
    private String fundamento;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(optional = false)
    private Funcionario funcionario;

    public Resolucion() {
    }

    public Resolucion(CasoFiscalizacion caso, EstadoCaso resultado, String fundamento, LocalDateTime fecha, Funcionario funcionario) {
        this.caso = caso;
        this.resultado = resultado;
        this.fundamento = fundamento;
        this.fecha = fecha;
        this.funcionario = funcionario;
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

    public EstadoCaso getResultado() {
        return resultado;
    }

    public void setResultado(EstadoCaso resultado) {
        this.resultado = resultado;
    }

    public String getFundamento() {
        return fundamento;
    }

    public void setFundamento(String fundamento) {
        this.fundamento = fundamento;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public void setFuncionario(Funcionario funcionario) {
        this.funcionario = funcionario;
    }

}
