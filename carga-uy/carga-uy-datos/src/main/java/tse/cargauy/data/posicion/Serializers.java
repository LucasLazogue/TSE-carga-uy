package tse.cargauy.data.posicion;

import tse.cargauy.dtos.EventoPosicionDto;
import tse.cargauy.entities.EventoPosicion;
import tse.cargauy.entities.NodoPeriferico;
import tse.cargauy.entities.Vehiculo;

public class Serializers {
    public static EventoPosicionDto toDto(EventoPosicion evento) {
        EventoPosicionDto dto = new EventoPosicionDto();
        dto.setId(evento.getId());
        dto.setIdEvento(evento.getIdEvento());
        dto.setNodo(evento.getNodo().getIdentificador());
        dto.setIdNodo(evento.getNodo().getId());
        dto.setMatricula(evento.getMatricula());
        dto.setIdVehiculo(evento.getVehiculo() == null ? null : evento.getVehiculo().getId());
        dto.setLatitud(evento.getLatitud());
        dto.setLongitud(evento.getLongitud());
        dto.setTimestampGeneracion(evento.getTimestampGeneracion());
        dto.setTimestampRecepcion(evento.getTimestampRecepcion());
        return dto;
    }

    public static EventoPosicion toEntity(EventoPosicionDto dto, NodoPeriferico nodo, Vehiculo vehiculo) {
        EventoPosicion evento = new EventoPosicion();
        evento.setIdEvento(dto.getIdEvento());
        evento.setNodo(nodo);
        evento.setMatricula(dto.getMatricula());
        evento.setVehiculo(vehiculo);
        evento.setLatitud(dto.getLatitud());
        evento.setLongitud(dto.getLongitud());
        evento.setTimestampGeneracion(dto.getTimestampGeneracion());
        evento.setTimestampRecepcion(dto.getTimestampRecepcion());
        return evento;
    }
}
