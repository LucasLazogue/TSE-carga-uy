package tse.cargauy.negocio.movil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.junit.jupiter.params.provider.ValueSource;

import tse.cargauy.data.eventoviaje.EventoViajeDAO;
import tse.cargauy.data.guia.GuiaDAO;
import tse.cargauy.data.viaje.ViajeDAO;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.ResultadoEventoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.entities.EventoViaje;
import tse.cargauy.entities.ResultadoEvento;
import tse.cargauy.entities.TipoEventoViaje;
import tse.cargauy.negocio.EventoFalso;
import tse.cargauy.negocio.viaje.ViajeIniciadoOFinalizado;

class AplicadorEventosEJBTest {

    private static final ZoneId ZONA = ZoneId.systemDefault();
    private static final Long CHOFER = 5001L;
    private static final int VOLUMEN = 18000;
    // la guia es de ayer: cualquier hora de las ultimas 24 h es valida, aunque el test corra cerca de la medianoche
    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    private final AplicadorEventosEJB ejb = new AplicadorEventosEJB();
    private final List<EventoViajeDto> registrados = new ArrayList<>();
    private final ViajeDto viaje = new ViajeDto(1L, 1L, CHOFER);
    private LocalDateTime fechaCambio;
    private LocalDateTime inicioCorregido;
    private boolean choferOcupado;
    private boolean vehiculoOcupado;
    private LocalDateTime hastaConsultado;
    private final EventoFalso<ViajeIniciadoOFinalizado> inicioOFin = new EventoFalso<>();

    @BeforeEach
    void preparar() {
        ejb.inicioOFin = inicioOFin;
        viaje.setId(2L);
        viaje.setEstado(EstadoViaje.ASIGNADO);
        viaje.setMatricula("STA1234");
        ejb.eventoDAO = new EventoViajeDAO() {
            @Override
            public boolean existe(String idEvento) {
                return registrados.stream().anyMatch(e -> e.getIdEvento().equals(idEvento));
            }

            @Override
            public void registrar(EventoViajeDto evento, Long idChofer, ResultadoEvento resultado, String detalle) {
                registrados.add(evento);
            }
        };
        ejb.viajeDAO = new ViajeDAO() {
            @Override
            public ViajeDto getViajeParaActualizar(Long id) {
                return id.equals(viaje.getId()) ? viaje : null;
            }

            @Override
            public void cambiarEstado(Long id, EstadoViaje estado, LocalDateTime fecha) {
                if (estado == EstadoViaje.EN_CURSO) {
                    viaje.setFechaInicio(fecha);
                } else if (estado == EstadoViaje.FINALIZADO) {
                    viaje.setFechaFin(fecha);
                }
                viaje.setEstado(estado);
                fechaCambio = fecha;
            }

            @Override
            public void corregirInicio(Long id, LocalDateTime fecha) {
                viaje.setFechaInicio(fecha);
                inicioCorregido = fecha;
            }

            @Override
            public boolean choferConOtroEnCurso(Long idViaje, Long idCiudadano, LocalDateTime desde, LocalDateTime hasta) {
                hastaConsultado = hasta;
                return choferOcupado;
            }

            @Override
            public boolean vehiculoConOtroEnCurso(Long idViaje, Long idVehiculo, LocalDateTime desde, LocalDateTime hasta) {
                return vehiculoOcupado;
            }
        };
        ejb.guiaDAO = new GuiaDAO() {
            @Override
            public GuiaDto getGuiaById(Long id) {
                GuiaDto guia = new GuiaDto();
                guia.setFecha(LocalDate.now(ZONA).minusDays(1));
                guia.setVolumen(VOLUMEN);
                return guia;
            }
        };
    }

    private static Instant haceMinutos(long minutos) {
        return AHORA.minus(Duration.ofMinutes(minutos));
    }

    private static LocalDateTime local(Instant instante) {
        return LocalDateTime.ofInstant(instante, ZONA);
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
        return evento(id, tipo, AHORA);
    }

    private EventoViajeDto incidente(String id, Instant fecha) {
        EventoViajeDto evento = evento(id, TipoEventoViaje.INCIDENTE, fecha);
        evento.setDescripcion("Pinchadura en ruta 5");
        return evento;
    }

    private EventoViajeDto descarga(String id, Integer kilos) {
        EventoViajeDto evento = evento(id, TipoEventoViaje.DESCARGA_PARCIAL);
        evento.setKilos(kilos);
        return evento;
    }

    private void enCursoDesde(Instant inicio) {
        viaje.setEstado(EstadoViaje.EN_CURSO);
        viaje.setFechaInicio(local(inicio));
    }

    private void finalizado(Instant inicio, Instant fin) {
        viaje.setEstado(EstadoViaje.FINALIZADO);
        viaje.setFechaInicio(local(inicio));
        viaje.setFechaFin(local(fin));
    }

    // ---- lo que ya hacia: duplicados, conflictos, fechas y aviso al tracking

    @Test
    void inicioUsaLaHoraDelEvento() {
        Instant hora = haceMinutos(30);

        ResultadoEventoDto resultado = ejb.aplicar(evento("e1", TipoEventoViaje.INICIO, hora), CHOFER);

        assertEquals(ResultadoEvento.APLICADO, resultado.getResultado());
        assertEquals(EstadoViaje.EN_CURSO, viaje.getEstado());
        assertEquals(local(hora), fechaCambio);
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
        assertEquals("e1", registrados.get(0).getIdEvento());
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
    void cargaSinInicioEsRechazada() {
        EventoViajeDto carga = evento("e1", TipoEventoViaje.CARGA_PARCIAL);
        carga.setKilos(1000);

        assertEquals(ResultadoEvento.RECHAZADO, ejb.aplicar(carga, CHOFER).getResultado());
    }

    @Test
    void fechaEnElFuturoEsRechazada() {
        ResultadoEventoDto resultado = ejb.aplicar(
                evento("e1", TipoEventoViaje.INICIO, AHORA.plus(Duration.ofDays(1))), CHOFER);

        assertEquals(ResultadoEvento.RECHAZADO, resultado.getResultado());
        assertEquals(EstadoViaje.ASIGNADO, viaje.getEstado());
    }

    // el inicio y el fin pueden llegar tarde: con la hora nueva se reasocian las posiciones del vehiculo (CU-10)
    @Test
    void inicioYFinAvisanParaReasociarLasPosiciones() {
        ejb.aplicar(evento("e1", TipoEventoViaje.INICIO, haceMinutos(10)), CHOFER);
        ejb.aplicar(evento("e2", TipoEventoViaje.FIN), CHOFER);

        assertEquals(List.of(new ViajeIniciadoOFinalizado(2L), new ViajeIniciadoOFinalizado(2L)), inicioOFin.disparados);
    }

    @Test
    void unEventoQueNoCambiaElHorarioNoAvisa() {
        enCursoDesde(haceMinutos(60));

        ejb.aplicar(incidente("e1", AHORA), CHOFER);
        ejb.aplicar(evento("e2", TipoEventoViaje.INICIO), CHOFER);

        assertTrue(inicioOFin.disparados.isEmpty());
    }

    // ---- D1: un viaje en curso por chofer y por vehiculo

    @Test
    void otroViajeDelChoferEnCursoRechazaElInicio() {
        choferOcupado = true;

        ResultadoEventoDto resultado = ejb.aplicar(evento("e1", TipoEventoViaje.INICIO), CHOFER);

        assertEquals(ResultadoEvento.RECHAZADO, resultado.getResultado());
        assertEquals("El chofer ya tiene otro viaje en curso a esa hora.", resultado.getMensaje());
        assertEquals(EstadoViaje.ASIGNADO, viaje.getEstado());
    }

    @Test
    void otroViajeDelVehiculoEnCursoRechazaElInicio() {
        vehiculoOcupado = true;

        ResultadoEventoDto resultado = ejb.aplicar(evento("e1", TipoEventoViaje.INICIO), CHOFER);

        assertEquals(ResultadoEvento.RECHAZADO, resultado.getResultado());
        assertEquals("El vehiculo STA1234 ya esta en otro viaje en curso a esa hora.", resultado.getMensaje());
        assertEquals(EstadoViaje.ASIGNADO, viaje.getEstado());
    }

    @Test
    void unViajeQueEmpiezaSeSuperponeHastaSinFin() {
        ejb.aplicar(evento("e1", TipoEventoViaje.INICIO), CHOFER);

        assertNull(hastaConsultado);
    }

    // ---- el inicio mas temprano fija la hora

    @Test
    void inicioMasTempranoCorrigeLaHora() {
        enCursoDesde(haceMinutos(10));

        ResultadoEventoDto resultado = ejb.aplicar(evento("e1", TipoEventoViaje.INICIO, haceMinutos(30)), CHOFER);

        assertEquals(ResultadoEvento.APLICADO, resultado.getResultado());
        assertEquals(local(haceMinutos(30)), inicioCorregido);
        assertEquals(EstadoViaje.EN_CURSO, viaje.getEstado());
        // solo se mira el tramo que se agrega, hasta el inicio que tenia
        assertEquals(local(haceMinutos(10)), hastaConsultado);
        assertEquals(List.of(new ViajeIniciadoOFinalizado(2L)), inicioOFin.disparados);
    }

    @Test
    void inicioMasTempranoEnUnViajeFinalizadoNoLoReabre() {
        finalizado(haceMinutos(60), haceMinutos(5));

        ejb.aplicar(evento("e1", TipoEventoViaje.INICIO, haceMinutos(90)), CHOFER);

        assertEquals(EstadoViaje.FINALIZADO, viaje.getEstado());
        assertEquals(local(haceMinutos(90)), inicioCorregido);
    }

    @Test
    void inicioPosteriorAlRegistradoEsRechazado() {
        enCursoDesde(haceMinutos(30));

        ResultadoEventoDto resultado = ejb.aplicar(evento("e1", TipoEventoViaje.INICIO, haceMinutos(10)), CHOFER);

        assertEquals(ResultadoEvento.RECHAZADO, resultado.getResultado());
        assertNull(inicioCorregido);
    }

    @Test
    void inicioMasTempranoQueSeSuperponeConOtroViajeEsRechazado() {
        enCursoDesde(haceMinutos(10));
        choferOcupado = true;

        ResultadoEventoDto resultado = ejb.aplicar(evento("e1", TipoEventoViaje.INICIO, haceMinutos(30)), CHOFER);

        assertEquals(ResultadoEvento.RECHAZADO, resultado.getResultado());
        assertNull(inicioCorregido);
    }

    // ---- D8: lo que ocurrio dentro del viaje se aplica siempre, aunque llegue tarde

    @Test
    void incidenteDentroDeUnViajeFinalizadoSeAplica() {
        finalizado(haceMinutos(60), haceMinutos(5));

        ResultadoEventoDto resultado = ejb.aplicar(incidente("e1", haceMinutos(30)), CHOFER);

        assertEquals(ResultadoEvento.APLICADO, resultado.getResultado());
        assertEquals(EstadoViaje.FINALIZADO, viaje.getEstado());
    }

    @Test
    void losExtremosDelViajeQuedanAdentro() {
        finalizado(haceMinutos(60), haceMinutos(5));

        assertEquals(ResultadoEvento.APLICADO, ejb.aplicar(incidente("e1", haceMinutos(60)), CHOFER).getResultado());
        assertEquals(ResultadoEvento.APLICADO, ejb.aplicar(incidente("e2", haceMinutos(5)), CHOFER).getResultado());
    }

    @Test
    void incidenteDespuesDelFinEsRechazado() {
        finalizado(haceMinutos(60), haceMinutos(30));

        ResultadoEventoDto resultado = ejb.aplicar(incidente("e1", haceMinutos(10)), CHOFER);

        assertEquals(ResultadoEvento.RECHAZADO, resultado.getResultado());
        assertTrue(resultado.getMensaje().startsWith("La hora del evento"));
    }

    @Test
    void incidenteAntesDelInicioEsRechazado() {
        enCursoDesde(haceMinutos(10));

        assertEquals(ResultadoEvento.RECHAZADO, ejb.aplicar(incidente("e1", haceMinutos(30)), CHOFER).getResultado());
    }

    @Test
    void finAntesDelInicioEsRechazado() {
        enCursoDesde(haceMinutos(10));

        ResultadoEventoDto resultado = ejb.aplicar(evento("e1", TipoEventoViaje.FIN, haceMinutos(30)), CHOFER);

        assertEquals(ResultadoEvento.RECHAZADO, resultado.getResultado());
        assertEquals(EstadoViaje.EN_CURSO, viaje.getEstado());
    }

    @Test
    void unSegundoFinEsRechazado() {
        finalizado(haceMinutos(60), haceMinutos(30));

        assertEquals(ResultadoEvento.RECHAZADO, ejb.aplicar(evento("e1", TipoEventoViaje.FIN), CHOFER).getResultado());
    }

    // ---- D4: descripcion y kilos

    @Test
    void incidenteSinDescripcionEsRechazado() {
        enCursoDesde(haceMinutos(10));

        ResultadoEventoDto resultado = ejb.aplicar(evento("e1", TipoEventoViaje.INCIDENTE), CHOFER);

        assertEquals(ResultadoEvento.RECHAZADO, resultado.getResultado());
        assertEquals("El incidente debe tener una descripcion.", resultado.getMensaje());
    }

    @Test
    void unaDescripcionEnBlancoCuentaComoQueNoHay() {
        enCursoDesde(haceMinutos(10));
        EventoViajeDto evento = evento("e1", TipoEventoViaje.INCIDENTE);
        evento.setDescripcion("   ");

        assertEquals(ResultadoEvento.RECHAZADO, ejb.aplicar(evento, CHOFER).getResultado());
        assertNull(registrados.get(0).getDescripcion());
    }

    @Test
    void laDescripcionSeGuardaSinEspaciosAlrededor() {
        enCursoDesde(haceMinutos(10));
        EventoViajeDto evento = incidente("e1", AHORA);
        evento.setDescripcion("  Pinchadura en ruta 5 \n");

        ejb.aplicar(evento, CHOFER);

        assertEquals("Pinchadura en ruta 5", registrados.get(0).getDescripcion());
    }

    @Test
    void descripcionLargaEsRechazadaYSeGuardaRecortada() {
        enCursoDesde(haceMinutos(10));
        EventoViajeDto evento = incidente("e1", AHORA);
        evento.setDescripcion("x".repeat(EventoViaje.DESCRIPCION_MAXIMA + 1));

        ResultadoEventoDto resultado = ejb.aplicar(evento, CHOFER);

        assertEquals(ResultadoEvento.RECHAZADO, resultado.getResultado());
        assertEquals(EventoViaje.DESCRIPCION_MAXIMA, registrados.get(0).getDescripcion().length());
    }

    @Test
    void descargaConKilosSeAplicaYLosGuarda() {
        enCursoDesde(haceMinutos(10));

        ResultadoEventoDto resultado = ejb.aplicar(descarga("e1", 3500), CHOFER);

        assertEquals(ResultadoEvento.APLICADO, resultado.getResultado());
        assertEquals(3500, registrados.get(0).getKilos());
    }

    @Test
    void descargaSinKilosEsRechazada() {
        enCursoDesde(haceMinutos(10));

        ResultadoEventoDto resultado = ejb.aplicar(descarga("e1", null), CHOFER);

        assertEquals(ResultadoEvento.RECHAZADO, resultado.getResultado());
        assertEquals("Debe indicar los kilos cargados o descargados.", resultado.getMensaje());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -5, VOLUMEN + 1 })
    void kilosFueraDeRangoSonRechazados(int kilos) {
        enCursoDesde(haceMinutos(10));

        assertEquals(ResultadoEvento.RECHAZADO, ejb.aplicar(descarga("e1", kilos), CHOFER).getResultado());
    }

    @Test
    void elVolumenDeLaGuiaEsElMaximo() {
        enCursoDesde(haceMinutos(10));

        assertEquals(ResultadoEvento.APLICADO, ejb.aplicar(descarga("e1", VOLUMEN), CHOFER).getResultado());
    }

    @Test
    void enUnIncidenteNoSeGuardanKilos() {
        enCursoDesde(haceMinutos(10));
        EventoViajeDto evento = incidente("e1", AHORA);
        evento.setKilos(100);

        ejb.aplicar(evento, CHOFER);

        assertNull(registrados.get(0).getKilos());
    }
}
