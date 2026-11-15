package tse.cargauy.negocio.historico;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.data.informe.InformeDAO;
import tse.cargauy.dtos.CasoHistoricoDto;
import tse.cargauy.dtos.ViajeHistoricoDto;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.TipoInfraccion;
import tse.cargauy.negocio.fiscalizacion.CasoCambiado;
import tse.cargauy.negocio.viaje.ViajeIniciadoOFinalizado;

class PublicadorHistoricoTest {

    private final Map<Long, ViajeHistoricoDto> finalizados = new HashMap<>();
    private final Map<Long, CasoHistoricoDto> casos = new HashMap<>();
    private final List<String> tipos = new ArrayList<>();
    private final List<String> cuerpos = new ArrayList<>();
    private RuntimeException alEnviar;

    private final PublicadorHistorico publicador = new PublicadorHistorico() {
        @Override
        void enviar(String tipo, String cuerpo) {
            if (alEnviar != null) {
                throw alEnviar;
            }
            tipos.add(tipo);
            cuerpos.add(cuerpo);
        }
    };

    @BeforeEach
    void iniciar() {
        publicador.informeDAO = new InformeDAO() {
            @Override
            public ViajeHistoricoDto getViajeHistorico(Long idViaje) {
                return finalizados.get(idViaje);
            }

            @Override
            public CasoHistoricoDto getCasoHistorico(Long idCaso) {
                return casos.get(idCaso);
            }
        };
    }

    @Test
    void elViajeFinalizadoSePublicaConTodoLoQueNecesitanLosReportes() {
        finalizados.put(21L, new ViajeHistoricoDto(21L, LocalDateTime.of(2026, 10, 1, 10, 0), 5L, 8L, 3L, "Granos", 2L, 30000));

        publicador.alCambiarViaje(new ViajeIniciadoOFinalizado(21L));

        assertEquals(List.of(PublicadorHistorico.VIAJE), tipos);
        ViajeHistoricoDto aviso = PublicadorHistorico.JSON.fromJson(cuerpos.get(0), ViajeHistoricoDto.class);
        assertEquals(21L, aviso.getIdViaje());
        assertEquals(LocalDateTime.of(2026, 10, 1, 10, 0), aviso.getFechaFin());
        assertEquals(5L, aviso.getIdEmpresa());
        assertEquals(8L, aviso.getIdVehiculo());
        assertEquals("Granos", aviso.getRubro());
        assertEquals(2L, aviso.getIdTipoCarga());
        assertEquals(30000, aviso.getVolumen());
    }

    @Test
    void elViajeQueRecienEmpiezaNoSePublica() {
        publicador.alCambiarViaje(new ViajeIniciadoOFinalizado(22L));

        assertTrue(tipos.isEmpty());
    }

    @Test
    void elCasoSePublicaConSuEstado() {
        casos.put(40L, new CasoHistoricoDto(40L, TipoInfraccion.EXCESO_CARGA, EstadoCaso.CONFIRMADA,
                LocalDateTime.of(2026, 10, 1, 10, 5), 2L));

        publicador.alCambiarCaso(new CasoCambiado(40L));

        assertEquals(List.of(PublicadorHistorico.CASO), tipos);
        CasoHistoricoDto aviso = PublicadorHistorico.JSON.fromJson(cuerpos.get(0), CasoHistoricoDto.class);
        assertEquals(40L, aviso.getIdCaso());
        assertEquals(TipoInfraccion.EXCESO_CARGA, aviso.getTipo());
        assertEquals(EstadoCaso.CONFIRMADA, aviso.getEstado());
        assertEquals(2L, aviso.getIdTipoCarga());
    }

    @Test
    void conElBrokerCaidoElViajeIgualQuedaFinalizado() {
        finalizados.put(21L, new ViajeHistoricoDto(21L, LocalDateTime.of(2026, 10, 1, 10, 0), 5L, 8L, 3L, "Granos", 2L, 30000));
        alEnviar = new IllegalStateException("sin broker");

        assertDoesNotThrow(() -> publicador.alCambiarViaje(new ViajeIniciadoOFinalizado(21L)));
    }

    @Test
    void conElBrokerCaidoElCasoIgualSeRegistra() {
        casos.put(40L, new CasoHistoricoDto(40L, TipoInfraccion.EXCESO_CARGA, EstadoCaso.NOTIFICADA,
                LocalDateTime.of(2026, 10, 1, 10, 5), 2L));
        alEnviar = new IllegalStateException("sin broker");

        assertDoesNotThrow(() -> publicador.alCambiarCaso(new CasoCambiado(40L)));
    }
}
