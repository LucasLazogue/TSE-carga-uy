package tse.cargauy.data.eventoviaje;

import java.time.Instant;

import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.entities.EventoViaje;
import tse.cargauy.entities.ResultadoEvento;

public class Serializers {
    public static EventoViaje toEntity(EventoViajeDto dto, Long idChofer, Instant recepcion,
            ResultadoEvento resultado, String detalle) {
        return new EventoViaje(dto.getIdEvento(), dto.getIdViaje(), idChofer, dto.getTipo(),
                dto.getTimestampGeneracion(), recepcion, resultado, detalle);
    }
}
