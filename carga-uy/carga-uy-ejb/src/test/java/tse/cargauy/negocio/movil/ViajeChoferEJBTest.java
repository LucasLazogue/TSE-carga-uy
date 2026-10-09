package tse.cargauy.negocio.movil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import tse.cargauy.data.viaje.ViajeDAO;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.dtos.ResultadoEventoDto;
import tse.cargauy.dtos.SyncRespuestaDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.ResultadoEvento;

class ViajeChoferEJBTest {

    private static EventoViajeDto evento(String id, Instant fecha) {
        EventoViajeDto evento = new EventoViajeDto();
        evento.setIdEvento(id);
        evento.setTimestampGeneracion(fecha);
        return evento;
    }

    @Test
    void aplicaLosEventosEnElOrdenEnQueSeGeneraron() {
        List<String> aplicados = new ArrayList<>();
        ViajeChoferEJB ejb = new ViajeChoferEJB() {
            @Override
            Long idUsuario() {
                return 5001L;
            }
        };
        ejb.aplicador = (evento, idChofer) -> {
            aplicados.add(evento.getIdEvento());
            return new ResultadoEventoDto(evento.getIdEvento(), ResultadoEvento.APLICADO, null);
        };
        ejb.viajeDAO = new ViajeDAO() {
            @Override
            public ViajeDto getActualDelChofer(Long idCiudadano) {
                return null;
            }
        };
        Instant ahora = Instant.now();

        SyncRespuestaDto respuesta = ejb.sincronizar(List.of(evento("segundo", ahora), evento("primero", ahora.minusSeconds(60))));

        assertEquals(List.of("primero", "segundo"), aplicados);
        assertEquals(2, respuesta.getResultados().size());
        assertNull(respuesta.getViaje());
    }
}
