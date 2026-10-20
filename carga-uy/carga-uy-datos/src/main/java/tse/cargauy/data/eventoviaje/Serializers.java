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

    public static EventoViajeDto toDto(EventoViaje entity) {
        EventoViajeDto dto = new EventoViajeDto();
        dto.setIdEvento(entity.getIdEvento());
        dto.setIdViaje(entity.getIdViaje());
        dto.setTipo(entity.getTipo());
        dto.setTimestampGeneracion(entity.getTimestampGeneracion());
        dto.setTimestampRecepcion(entity.getTimestampRecepcion());
        dto.setResultado(entity.getResultado());
        dto.setDetalle(entity.getDetalle());
        return dto;
    }
}
