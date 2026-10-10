package tse.cargauy.negocio.movil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import tse.cargauy.data.eventoviaje.EventoViajeDAO;
import tse.cargauy.data.guia.GuiaDAO;
import tse.cargauy.data.viaje.ViajeDAO;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.ResultadoEventoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.entities.ResultadoEvento;
import tse.cargauy.entities.TipoEventoViaje;

class AplicadorEventosEJBTest {

    private static final ZoneId ZONA = ZoneId.systemDefault();
    private static final Long CHOFER = 5001L;

    private final AplicadorEventosEJB ejb = new AplicadorEventosEJB();
    private final List<String> registrados = new ArrayList<>();
    private final ViajeDto viaje = new ViajeDto(1L, 1L, CHOFER);
    private LocalDateTime fechaCambio;

    @BeforeEach
    void preparar() {
        viaje.setId(2L);
        viaje.setEstado(EstadoViaje.ASIGNADO);
        ejb.eventoDAO = new EventoViajeDAO() {
            @Override
            public boolean existe(String idEvento) {
                return registrados.contains(idEvento);
            }

            @Override
            public void registrar(EventoViajeDto evento, Long idChofer, ResultadoEvento resultado, String detalle) {
                registrados.add(evento.getIdEvento());
            }
        };
        ejb.viajeDAO = new ViajeDAO() {
            @Override
            public ViajeDto getViajeById(Long id) {
                return id.equals(viaje.getId()) ? viaje : null;
            }

            @Override
            public void cambiarEstado(Long id, EstadoViaje estado, LocalDateTime fecha) {
                viaje.setEstado(estado);
                fechaCambio = fecha;
            }
        };
        ejb.guiaDAO = new GuiaDAO() {
            @Override
            public GuiaDto getGuiaById(Long id) {
                GuiaDto guia = new GuiaDto();
                guia.setFecha(LocalDate.now(ZONA));
                return guia;
            }
        };
    }

    private EventoViajeDto evento(String id, TipoEventoViaje tipo, Instant fecha) {
        EventoViajeDto evento = new EventoViajeDto();
        evento.setIdEvento(id);
        evento.setIdViaje(viaje.getId());
        evento.setTipo(tipo);
        evento.setTimestampGeneracion(fecha);
        return evento;
    }

    private EventoViajeDto evento(String id, TipoEventoViaje tipo) {
        return evento(id, tipo, Instant.now());
    }

    @ParameterizedTest
    @CsvSource({
        "ASIGNADO, INICIO, EN_CURSO",
        "ASIGNADO, CARGA_PARCIAL, ",
        "ASIGNADO, DESCARGA_PARCIAL, ",
        "ASIGNADO, INCIDENTE, ",
        "ASIGNADO, FIN, ",
        "EN_CURSO, INICIO, ",
        "EN_CURSO, CARGA_PARCIAL, EN_CURSO",
        "EN_CURSO, DESCARGA_PARCIAL, EN_CURSO",
        "EN_CURSO, INCIDENTE, EN_CURSO",
        "EN_CURSO, FIN, FINALIZADO",
        "FINALIZADO, INICIO, ",
        "FINALIZADO, CARGA_PARCIAL, ",
        "FINALIZADO, DESCARGA_PARCIAL, ",
        "FINALIZADO, INCIDENTE, ",
        "FINALIZADO, FIN, "
    })
    void transicion(EstadoViaje actual, TipoEventoViaje tipo, EstadoViaje esperado) {
        assertEquals(esperado, AplicadorEventosEJB.transicion(actual, tipo));
    }

    @Test
    void inicioUsaLaHoraDelEvento() {
        Instant hora = Instant.now().minus(Duration.ofMinutes(30)).truncatedTo(ChronoUnit.SECONDS);
        // si hace 30 minutos todavia era ayer, la guia de hoy lo rechazaria
        if (LocalDate.ofInstant(hora, ZONA).isBefore(LocalDate.now(ZONA))) {
            hora = Instant.now();
        }

        ResultadoEventoDto resultado = ejb.aplicar(evento("e1", TipoEventoViaje.INICIO, hora), CHOFER);

        assertEquals(ResultadoEvento.APLICADO, resultado.getResultado());
        assertEquals(EstadoViaje.EN_CURSO, viaje.getEstado());
        assertEquals(LocalDateTime.ofInstant(hora, ZONA), fechaCambio);
    }

    @Test
    void elMismoEventoDosVecesEsDuplicado() {
        ejb.aplicar(evento("e1", TipoEventoViaje.INICIO), CHOFER);

        assertEquals(ResultadoEvento.DUPLICADO, ejb.aplicar(evento("e1", TipoEventoViaje.INICIO), CHOFER).getResultado());
        assertEquals(1, registrados.size());
    }

    @Test
    void viajeDeOtroChoferEsConflictoYSeRegistra() {
        ResultadoEventoDto resultado = ejb.aplicar(evento("e1", TipoEventoViaje.INICIO), 999L);

        assertEquals(ResultadoEvento.CONFLICTO, resultado.getResultado());
        assertTrue(registrados.contains("e1"));
        assertEquals(EstadoViaje.ASIGNADO, viaje.getEstado());
    }

    @Test
    void viajeInexistenteEsConflicto() {
        EventoViajeDto evento = evento("e1", TipoEventoViaje.INICIO);
        evento.setIdViaje(99L);

        assertEquals(ResultadoEvento.CONFLICTO, ejb.aplicar(evento, CHOFER).getResultado());
    }

    @Test
    void finSinInicioEsRechazado() {
        ResultadoEventoDto resultado = ejb.aplicar(evento("e1", TipoEventoViaje.FIN), CHOFER);

        assertEquals(ResultadoEvento.RECHAZADO, resultado.getResultado());
        assertEquals("El viaje esta ASIGNADO y no admite el evento FIN.", resultado.getMensaje());
    }

    @Test
    void fechaEnElFuturoEsRechazada() {
        ResultadoEventoDto resultado = ejb.aplicar(
                evento("e1", TipoEventoViaje.INICIO, Instant.now().plus(Duration.ofDays(1))), CHOFER);

        assertEquals(ResultadoEvento.RECHAZADO, resultado.getResultado());
        assertEquals(EstadoViaje.ASIGNADO, viaje.getEstado());
    }
}
