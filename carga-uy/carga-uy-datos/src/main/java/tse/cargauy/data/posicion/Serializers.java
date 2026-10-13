package tse.cargauy.data.posicion;

import java.util.Date;

import org.bson.Document;

import tse.cargauy.dtos.EventoPosicionDto;

// el vehiculo y el nodo se guardan por su identificador, sin claves foraneas: estan en PostgreSQL (DA07)
public class Serializers {
    public static EventoPosicionDto toDto(Document d) {
        EventoPosicionDto dto = new EventoPosicionDto();
        dto.setId(d.getObjectId("_id").toHexString());
        dto.setIdEvento(d.getString("idEvento"));
        dto.setNodo(d.getString("nodo"));
        dto.setIdNodo(d.getLong("idNodo"));
        dto.setMatricula(d.getString("matricula"));
        dto.setIdVehiculo(d.getLong("idVehiculo"));
        dto.setIdViaje(d.getLong("idViaje"));
        dto.setLatitud(d.getDouble("latitud"));
        dto.setLongitud(d.getDouble("longitud"));
        dto.setTimestampGeneracion(d.getDate("timestampGeneracion").toInstant());
        dto.setTimestampRecepcion(d.getDate("timestampRecepcion").toInstant());
        dto.setProcesada(d.getBoolean("procesada", false));
        return dto;
    }

    // MongoDB guarda las fechas con precision de milisegundos
    public static Document toDocument(EventoPosicionDto dto) {
        return new Document("idEvento", dto.getIdEvento())
                .append("idNodo", dto.getIdNodo())
                .append("nodo", dto.getNodo())
                .append("matricula", dto.getMatricula())
                .append("idVehiculo", dto.getIdVehiculo())
                .append("idViaje", dto.getIdViaje())
                .append("latitud", dto.getLatitud())
                .append("longitud", dto.getLongitud())
                .append("timestampGeneracion", Date.from(dto.getTimestampGeneracion()))
                .append("timestampRecepcion", Date.from(dto.getTimestampRecepcion()))
                .append("procesada", dto.isProcesada());
    }
}
