package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.Instant;

import tse.cargauy.entities.ResultadoEvento;
import tse.cargauy.entities.TipoEventoViaje;

public class EventoViajeDto implements Serializable{
    private static final long serialVersionUID = 1L;

    private String idEvento;
    private Long idViaje;
    private TipoEventoViaje tipo;
    private Instant timestampGeneracion;
    private Instant timestampRecepcion;
    private ResultadoEvento resultado;
    private String detalle;

    public EventoViajeDto () {
    }

    public String getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(String idEvento) {
        this.idEvento = idEvento;
    }

    public Long getIdViaje() {
        return idViaje;
    }

    public void setIdViaje(Long idViaje) {
        this.idViaje = idViaje;
    }

    public TipoEventoViaje getTipo() {
        return tipo;
    }

    public void setTipo(TipoEventoViaje tipo) {
        this.tipo = tipo;
    }

    public Instant getTimestampGeneracion() {
        return timestampGeneracion;
    }

    public void setTimestampGeneracion(Instant timestampGeneracion) {
        this.timestampGeneracion = timestampGeneracion;
    }

    public Instant getTimestampRecepcion() {
        return timestampRecepcion;
    }

    public void setTimestampRecepcion(Instant timestampRecepcion) {
        this.timestampRecepcion = timestampRecepcion;
    }

    public ResultadoEvento getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoEvento resultado) {
        this.resultado = resultado;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
}
