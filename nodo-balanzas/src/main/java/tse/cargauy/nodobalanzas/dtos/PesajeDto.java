package tse.cargauy.nodobalanzas.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PesajeDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String matricula;
    private String rutEmpresa;
    private LocalDateTime fechaHora;
    private int pesoRegistrado;
    private BalanzaDto balanza;
    private PeajeDto peaje;

    public PesajeDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BalanzaDto getBalanza() {
        return balanza;
    }

    public void setBalanza(BalanzaDto balanza) {
        this.balanza = balanza;
    }

    public PeajeDto getPeaje() {
        return peaje;
    }

    public void setPeaje(PeajeDto peaje) {
        this.peaje = peaje;
    }
}
