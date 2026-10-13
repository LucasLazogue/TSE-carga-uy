package tse.cargauy.data.posicion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Date;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.EventoPosicionDto;

class SerializersTest {

    @Test
    void elDocumentoConservaLosDatosDeLaPosicion() {
        EventoPosicionDto evento = evento(11L);
        Document documento = Serializers.toDocument(evento);
        ObjectId id = new ObjectId();
        documento.put("_id", id);

        EventoPosicionDto leido = Serializers.toDto(documento);

        assertEquals(id.toHexString(), leido.getId());
        assertEquals("ev-1", leido.getIdEvento());
        assertEquals("tracking-1", leido.getNodo());
        assertEquals(3L, leido.getIdNodo());
        assertEquals("STA1234", leido.getMatricula());
        assertEquals(11L, leido.getIdVehiculo());
        assertEquals(-34.9, leido.getLatitud());
        assertEquals(-56.1, leido.getLongitud());
        assertEquals(evento.getTimestampGeneracion(), leido.getTimestampGeneracion());
        assertEquals(evento.getTimestampRecepcion(), leido.getTimestampRecepcion());
        assertTrue(leido.isProcesada());
    }

    @Test
    void unDocumentoSinLaMarcaSeLeeComoSinProcesar() {
        Document documento = Serializers.toDocument(evento(11L));
        documento.remove("procesada");
        documento.put("_id", new ObjectId());

        assertFalse(Serializers.toDto(documento).isProcesada());
    }

    @Test
    void unaMatriculaDesconocidaSeGuardaSinVehiculo() {
        Document documento = Serializers.toDocument(evento(null));
        documento.put("_id", new ObjectId());

        assertNull(Serializers.toDto(documento).getIdVehiculo());
    }

    // el indice unico es sobre estos dos campos: si cambian de nombre, la ingesta deja de descartar duplicados
    @Test
    void elNodoYElIdDelEventoVanEnLosCamposDelIndiceUnico() {
        Document documento = Serializers.toDocument(evento(11L));

        assertEquals(3L, documento.getLong("idNodo"));
        assertEquals("ev-1", documento.getString("idEvento"));
    }

    @Test
    void lasFechasSeGuardanComoFechasParaPoderOrdenar() {
        Document documento = Serializers.toDocument(evento(11L));

        assertEquals(Date.from(Instant.parse("2026-10-06T12:00:00Z")), documento.getDate("timestampGeneracion"));
    }

    private static EventoPosicionDto evento(Long idVehiculo) {
        EventoPosicionDto evento = new EventoPosicionDto();
        evento.setIdEvento("ev-1");
        evento.setNodo("tracking-1");
        evento.setIdNodo(3L);
        evento.setMatricula("STA1234");
        evento.setIdVehiculo(idVehiculo);
        evento.setLatitud(-34.9);
        evento.setLongitud(-56.1);
        evento.setTimestampGeneracion(Instant.parse("2026-10-06T12:00:00Z"));
        evento.setTimestampRecepcion(Instant.parse("2026-10-06T12:00:03.250Z"));
        evento.setProcesada(true);
        return evento;
    }
}
