package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.Instant;

import tse.cargauy.entities.TipoEventoViaje;

public class EventoViajeDto implements Serializable{
    private static final long serialVersionUID = 1L;

    private String idEvento;
    private Long idViaje;
    private TipoEventoViaje tipo;
    private Instant timestampGeneracion;

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
}
