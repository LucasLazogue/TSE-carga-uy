package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;

import tse.cargauy.entities.EstadoCaso;

public class ResolucionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private EstadoCaso resultado;
    private String fundamento;
    private LocalDateTime fecha;
    private String cedulaFuncionario;

    public ResolucionDto() {
    }

    public ResolucionDto(EstadoCaso resultado, String fundamento) {
        this.resultado = resultado;
        this.fundamento = fundamento;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCedulaFuncionario() {
        return cedulaFuncionario;
    }

    public void setCedulaFuncionario(String cedulaFuncionario) {
        this.cedulaFuncionario = cedulaFuncionario;
    }
}
