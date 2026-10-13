package tse.cargauy.dtos;

import java.io.Serializable;

import tse.cargauy.entities.ResultadoEvento;

public class ResultadoEventoDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String idEvento;
    private ResultadoEvento resultado;
    private String mensaje;

    public ResultadoEventoDto() {
    }

    public ResultadoEventoDto(String idEvento, ResultadoEvento resultado, String mensaje) {
        this.idEvento = idEvento;
        this.resultado = resultado;
        this.mensaje = mensaje;
    }

    public String getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(String idEvento) {
        this.idEvento = idEvento;
    }

    public ResultadoEvento getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoEvento resultado) {
        this.resultado = resultado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
