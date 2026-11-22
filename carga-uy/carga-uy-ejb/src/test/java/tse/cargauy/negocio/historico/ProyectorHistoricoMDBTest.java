package tse.cargauy.negocio.historico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import tse.cargauy.dtos.CasoHistoricoDto;
import tse.cargauy.dtos.ViajeHistoricoDto;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.TipoInfraccion;
import tse.cargauy.historico.HistoricoDAO;

class ProyectorHistoricoMDBTest {

    private final ProyectorHistoricoMDB proyector = new ProyectorHistoricoMDB();
    private final List<ViajeHistoricoDto> viajes = new ArrayList<>();
    private final List<CasoHistoricoDto> casos = new ArrayList<>();
    private RuntimeException alGuardar;

    @BeforeEach
    void iniciar() {
        proyector.historicoDAO = new HistoricoDAO() {
            @Override
            public void guardarViaje(ViajeHistoricoDto viaje) {
                if (alGuardar != null) {
                    throw alGuardar;
                }
                viajes.add(viaje);
            }

            @Override
            public void guardarCaso(CasoHistoricoDto caso) {
                casos.add(caso);
            }
        };
    }

    private static Message texto(String tipo, String cuerpo) {
        return (Message) Proxy.newProxyInstance(ProyectorHistoricoMDBTest.class.getClassLoader(),
                new Class<?>[] { TextMessage.class }, (proxy, metodo, args) -> switch (metodo.getName()) {
                    case "getStringProperty" -> tipo;
                    case "getText" -> cuerpo;
                    default -> null;
                });
    }

    @Test
    void elAvisoDeUnViajeGuardaSuFila() {
        ViajeHistoricoDto viaje = new ViajeHistoricoDto(21L, LocalDateTime.of(2026, 10, 1, 10, 0), 5L, 8L, 3L, "Granos", 2L, 30000);

        proyector.onMessage(texto(PublicadorHistorico.VIAJE, PublicadorHistorico.JSON.toJson(viaje)));

        assertEquals(1, viajes.size());
        assertEquals(21L, viajes.get(0).getIdViaje());
        assertEquals("Granos", viajes.get(0).getRubro());
        assertTrue(casos.isEmpty());
    }

    @Test
    void elAvisoDeUnCasoGuardaSuFila() {
        CasoHistoricoDto caso = new CasoHistoricoDto(40L, TipoInfraccion.EXCESO_CARGA, EstadoCaso.DESESTIMADA,
                LocalDateTime.of(2026, 10, 1, 10, 5), null);

        proyector.onMessage(texto(PublicadorHistorico.CASO, PublicadorHistorico.JSON.toJson(caso)));

        assertEquals(1, casos.size());
        assertEquals(EstadoCaso.DESESTIMADA, casos.get(0).getEstado());
        assertTrue(viajes.isEmpty());
    }

    @Test
    void unAvisoMalFormadoSeDescartaSinReintentar() {
        proyector.onMessage(texto(PublicadorHistorico.VIAJE, "{no es json"));
        proyector.onMessage(texto(null, "{}"));
        proyector.onMessage(texto("otro", "{}"));
        proyector.onMessage((Message) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { Message.class }, (proxy, metodo, args) -> null));

        assertTrue(viajes.isEmpty());
        assertTrue(casos.isEmpty());
    }

    @Test
    void siLaBaseDelHistoricoNoEstaElAvisoVuelveALaCola() {
        alGuardar = new IllegalStateException("sin conexion");
        ViajeHistoricoDto viaje = new ViajeHistoricoDto(21L, LocalDateTime.of(2026, 10, 1, 10, 0), 5L, 8L, 3L, "Granos", 2L, 30000);
        Message aviso = texto(PublicadorHistorico.VIAJE, PublicadorHistorico.JSON.toJson(viaje));

        assertThrows(IllegalStateException.class, () -> proyector.onMessage(aviso));
    }
}
