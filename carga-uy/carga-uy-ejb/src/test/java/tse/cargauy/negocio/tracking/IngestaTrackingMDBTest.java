package tse.cargauy.negocio.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import tse.cargauy.dtos.EventoPosicionDto;
import tse.cargauy.negocio.MensajesJms;

class IngestaTrackingMDBTest {

    private static final String TRAZA = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String TRACEPARENT = "00-" + TRAZA + "-00f067aa0ba902b7-01";
    private static final String JSON = """
            {"idEvento":"e-1","nodo":"nodo-1","matricula":"SBA1234","latitud":-34.9,"longitud":-56.1,\
            "timestampGeneracion":"2026-10-06T12:00:00Z"}""";

    private final IngestaTrackingMDB mdb = new IngestaTrackingMDB();
    private final SimpleMeterRegistry registro = new SimpleMeterRegistry();
    private final List<EventoPosicionDto> procesados = new ArrayList<>();
    private final List<String> trazas = new ArrayList<>();

    private Function<EventoPosicionDto, ResultadoIngesta> ingesta = e -> ResultadoIngesta.REGISTRADO;

    @BeforeEach
    void setUp() {
        mdb.openTelemetry = OpenTelemetry.propagating(ContextPropagators.create(W3CTraceContextPropagator.getInstance()));
        mdb.tracer = mdb.openTelemetry.getTracer("test");
        MetricasIngesta metricas = new MetricasIngesta();
        metricas.registro = registro;
        metricas.crear();
        mdb.metricas = metricas;
        mdb.ingesta = evento -> {
            procesados.add(evento);
            trazas.add(Span.current().getSpanContext().getTraceId());
            return ingesta.apply(evento);
        };
    }

    private double eventos(String resultado) {
        return registro.counter("ingesta.eventos", "resultado", resultado).count();
    }

    private double errores(String tipo) {
        return registro.counter("ingesta.errores", "tipo", tipo).count();
    }

    @Test
    void leeElEventoDelMensajeYLoProcesa() {
        mdb.onMessage(MensajesJms.texto(JSON));

        assertEquals(1, procesados.size());
        EventoPosicionDto evento = procesados.get(0);
        assertEquals("e-1", evento.getIdEvento());
        assertEquals("nodo-1", evento.getNodo());
        assertEquals("SBA1234", evento.getMatricula());
        assertEquals(-34.9, evento.getLatitud());
        assertEquals(-56.1, evento.getLongitud());
        assertEquals(Instant.parse("2026-10-06T12:00:00Z"), evento.getTimestampGeneracion());
        assertEquals(1, eventos("REGISTRADO"));
    }

    // la traza del central continua la del nodo, que viaja en la propiedad traceparent del mensaje
    @Test
    void continuaLaTrazaDelNodo() {
        mdb.onMessage(MensajesJms.texto(JSON, Map.of("traceparent", TRACEPARENT), System.currentTimeMillis()));

        assertEquals(List.of(TRAZA), trazas);
    }

    // un reenvio del mismo evento (R6) no es un error: se cuenta como duplicado
    @Test
    void unDuplicadoSeCuentaPeroNoEsError() {
        ingesta = e -> ResultadoIngesta.DUPLICADO;

        mdb.onMessage(MensajesJms.texto(JSON));

        assertEquals(1, eventos("DUPLICADO"));
        assertEquals(0, registro.find("ingesta.errores").counters().size());
    }

    @Test
    void unNodoDesconocidoODeshabilitadoSeCuentaComoError() {
        ingesta = e -> ResultadoIngesta.NODO_DESCONOCIDO;
        mdb.onMessage(MensajesJms.texto(JSON));
        ingesta = e -> ResultadoIngesta.NODO_NO_HABILITADO;
        mdb.onMessage(MensajesJms.texto(JSON));

        assertEquals(1, errores("NODO_DESCONOCIDO"));
        assertEquals(1, errores("NODO_NO_HABILITADO"));
    }

    // reintentar un mensaje mal formado no lo arregla: se descarta sin excepcion para no trancar la cola
    @Test
    void unMensajeMalFormadoSeDescartaSinReintentar() {
        mdb.onMessage(MensajesJms.texto("no es json"));
        mdb.onMessage(MensajesJms.texto("{\"idEvento\":\"e-1\"}"));
        mdb.onMessage(MensajesJms.texto(JSON.replace("2026-10-06T12:00:00Z", "ayer")));
        mdb.onMessage(MensajesJms.texto(JSON.replace("-34.9", "\"-34.9\"")));
        mdb.onMessage(MensajesJms.noTexto());
        mdb.onMessage(MensajesJms.ilegible());

        assertTrue(procesados.isEmpty());
        assertEquals(6, eventos("MAL_FORMADO"));
        assertEquals(6, errores("MAL_FORMADO"));
    }

    // si falla la base la excepcion sigue de largo: el broker reentrega el mensaje
    @Test
    void unaFallaAlProcesarSeRelanzaParaQueElBrokerReentregue() {
        IllegalStateException falla = new IllegalStateException("sin base");
        ingesta = e -> {
            throw falla;
        };

        IllegalStateException lanzada = assertThrows(IllegalStateException.class,
                () -> mdb.onMessage(MensajesJms.texto(JSON)));

        assertSame(falla, lanzada);
        assertEquals(1, eventos("ERROR"));
        assertEquals(1, errores("IllegalStateException"));
    }
}
