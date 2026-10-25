package tse.cargauy.nodobalanzas.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PesajeDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String idRegistro;
    private String matricula;
    private LocalDateTime fechaHora;
    private int pesoRegistrado;
    private BalanzaDto balanza;
    private PeajeDto peaje;

    public PesajeDto() {
    }

    public PesajeDto(String idRegistro, String matricula, LocalDateTime fechaHora, int pesoRegistrado) {
        this.idRegistro = idRegistro;
        this.matricula = matricula;
        this.fechaHora = fechaHora;
        this.pesoRegistrado = pesoRegistrado;
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
