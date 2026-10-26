package tse.cargauy.data.pesaje;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.PesajeDto;

class SerializersTest {

    @Test
    void laEntidadConservaElPesajeConSuBalanzaYSuPeaje() {
        PesajeDto leido = Serializers.toDto(Serializers.toEntity(pesaje()));

        assertEquals("balanzas-sur/balanza-r5-km30/1001", leido.getIdOrigen());
        assertEquals(1L, leido.getIdNodo());
        assertEquals(3L, leido.getIdVehiculo());
        assertNull(leido.getIdViaje());
        assertEquals("SCC9012", leido.getMatricula());
        assertEquals(LocalDateTime.of(2026, 10, 22, 9, 10), leido.getFechaHora());
        assertEquals(36500, leido.getPesoRegistrado());
        assertEquals("balanza-r5-km30", leido.getCodigoBalanza());
        assertEquals("HACIA_MONTEVIDEO", leido.getSentido());
        assertEquals("R5-30", leido.getCodigoPeaje());
        assertEquals("Peaje Ruta 5 km 30", leido.getNombrePeaje());
        assertEquals("5", leido.getRuta());
        assertEquals(30, leido.getKm());
        assertEquals(-34.66, leido.getLatitud());
        assertEquals(-56.22, leido.getLongitud());
        assertEquals(LocalDateTime.of(2026, 10, 25, 12, 0), leido.getRecibidoEn());
    }

    private static PesajeDto pesaje() {
        PesajeDto pesaje = new PesajeDto();
        pesaje.setIdOrigen("balanzas-sur/balanza-r5-km30/1001");
        pesaje.setIdNodo(1L);
        pesaje.setIdVehiculo(3L);
        pesaje.setMatricula("SCC9012");
        pesaje.setFechaHora(LocalDateTime.of(2026, 10, 22, 9, 10));
        pesaje.setPesoRegistrado(36500);
        pesaje.setCodigoBalanza("balanza-r5-km30");
        pesaje.setSentido("HACIA_MONTEVIDEO");
        pesaje.setCodigoPeaje("R5-30");
        pesaje.setNombrePeaje("Peaje Ruta 5 km 30");
        pesaje.setRuta("5");
        pesaje.setKm(30);
        pesaje.setLatitud(-34.66);
        pesaje.setLongitud(-56.22);
        pesaje.setRecibidoEn(LocalDateTime.of(2026, 10, 25, 12, 0));
        return pesaje;
    }
}
