package tse.cargauy.adaptadores.balanzas;

import java.time.LocalDateTime;

public class PesajeNodo {

    private String idRegistro;
    private String matricula;
    private LocalDateTime fechaHora;
    private int pesoRegistrado;
    private BalanzaNodo balanza;
    private PeajeNodo peaje;

    public PesajeNodo() {
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

    public BalanzaNodo getBalanza() {
        return balanza;
    }

    public void setBalanza(BalanzaNodo balanza) {
        this.balanza = balanza;
    }

    public PeajeNodo getPeaje() {
        return peaje;
    }

    public void setPeaje(PeajeNodo peaje) {
        this.peaje = peaje;
    }
}
