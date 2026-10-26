package tse.cargauy.negocio.movil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.data.guia.GuiaDAO;
import tse.cargauy.data.viaje.ViajeDAO;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.ResultadoEventoDto;
import tse.cargauy.dtos.SyncRespuestaDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.dtos.ViajeMovilDto;
import tse.cargauy.entities.ResultadoEvento;

class ViajeChoferEJBTest {

    private final List<String> aplicados = new ArrayList<>();
    private final List<ViajeDto> activos = new ArrayList<>();
    private final ViajeChoferEJB ejb = new ViajeChoferEJB() {
        @Override
        Long idUsuario() {
            return 5001L;
        }
    };

    @BeforeEach
    void preparar() {
        ejb.aplicador = (evento, idChofer) -> {
            aplicados.add(evento.getIdEvento());
            return new ResultadoEventoDto(evento.getIdEvento(), ResultadoEvento.APLICADO, null);
        };
        ejb.viajeDAO = new ViajeDAO() {
            @Override
            public List<ViajeDto> getActivosDelChofer(Long idCiudadano) {
                return activos;
            }
        };
        ejb.guiaDAO = new GuiaDAO() {
            @Override
            public GuiaDto getGuiaById(Long id) {
                GuiaDto guia = new GuiaDto();
                guia.setId(id);
                return guia;
            }
        };
    }

    private static EventoViajeDto evento(String id, Instant fecha) {
        EventoViajeDto evento = new EventoViajeDto();
        evento.setIdEvento(id);
        evento.setTimestampGeneracion(fecha);
        return evento;
    }

    private static ViajeDto viaje(Long id, Long idGuia) {
        ViajeDto viaje = new ViajeDto(idGuia, 1L, 5001L);
        viaje.setId(id);
        return viaje;
    }

    @Test
    void aplicaLosEventosEnElOrdenEnQueSeGeneraron() {
        Instant ahora = Instant.now();

        SyncRespuestaDto respuesta = ejb.sincronizar(List.of(evento("segundo", ahora), evento("primero", ahora.minusSeconds(60))));

        assertEquals(List.of("primero", "segundo"), aplicados);
        assertEquals(2, respuesta.getResultados().size());
        assertTrue(respuesta.getViajes().isEmpty());
    }

    @Test
    void devuelveCadaViajeConSuGuiaEnElOrdenDelDao() {
        activos.add(viaje(7L, 70L));
        activos.add(viaje(8L, 80L));

        List<ViajeMovilDto> viajes = ejb.getViajes();

        assertEquals(List.of(7L, 8L), viajes.stream().map(v -> v.getViaje().getId()).toList());
        assertEquals(List.of(70L, 80L), viajes.stream().map(v -> v.getGuia().getId()).toList());
    }

    @Test
    void elSyncDevuelveLosViajesComoQuedaron() {
        activos.add(viaje(7L, 70L));

        SyncRespuestaDto respuesta = ejb.sincronizar(List.of(evento("e1", Instant.now())));

        assertEquals(1, respuesta.getViajes().size());
    }
}
