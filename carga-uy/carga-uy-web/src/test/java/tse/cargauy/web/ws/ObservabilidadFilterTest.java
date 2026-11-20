package tse.cargauy.web.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.MDC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;
import tse.cargauy.web.ws.casos.CasosRest;

// AC09: el tiempo de respuesta por operacion; AC23: la traza de WildFly como correlacion de los logs
class ObservabilidadFilterTest {

    private static final String TRAZA = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN = "00f067aa0ba902b7";

    private final ObservabilidadFilter filtro = new ObservabilidadFilter();
    private final SimpleMeterRegistry registro = new SimpleMeterRegistry();
    private final ContainerRequestContext pedido = mock(ContainerRequestContext.class);
    private final ContainerResponseContext respuesta = mock(ContainerResponseContext.class);

    @BeforeEach
    void preparar() {
        filtro.registro = registro;
        Map<String, Object> propiedades = new HashMap<>();
        doAnswer(i -> propiedades.put(i.getArgument(0), i.getArgument(1))).when(pedido).setProperty(anyString(), org.mockito.ArgumentMatchers.any());
        when(pedido.getProperty(anyString())).thenAnswer(i -> propiedades.get(i.<String>getArgument(0)));
        when(pedido.getMethod()).thenReturn("GET");
        when(respuesta.getStatus()).thenReturn(200);
    }

    private Timer timer(String operacion) {
        return registro.find("http.servidor").tag("operacion", operacion).tag("metodo", "GET").tag("estado", "200").timer();
    }

    // la serie es por clase y metodo del recurso, no por URL: los identificadores no abren una serie cada uno
    @Test
    void mideElTiempoPorOperacion() throws Exception {
        ResourceInfo recurso = mock(ResourceInfo.class);
        when(recurso.getResourceMethod()).thenReturn(CasosRest.class.getMethods()[0]);
        doAnswer(i -> CasosRest.class).when(recurso).getResourceClass();
        filtro.recurso = recurso;

        filtro.filter(pedido);
        filtro.filter(pedido, respuesta);

        String operacion = "CasosRest." + CasosRest.class.getMethods()[0].getName();
        assertNotNull(timer(operacion));
        assertEquals(1, timer(operacion).count());
    }

    @Test
    void sinRecursoLaOperacionEsDesconocida() {
        filtro.filter(pedido);
        filtro.filter(pedido, respuesta);

        assertEquals(1, timer("desconocida").count());
    }

    // si el filtro de entrada no corrio (por ejemplo el pedido fallo antes) no hay nada que medir
    @Test
    void sinInicioNoMide() {
        filtro.filter(pedido, respuesta);

        assertNull(registro.find("http.servidor").timer());
    }

    @Test
    void ponePorPedidoLaTrazaEnLosLogsYLaSaca() {
        SpanContext contexto = SpanContext.create(TRAZA, SPAN, TraceFlags.getSampled(), TraceState.getDefault());
        try (Scope alcance = Span.wrap(contexto).makeCurrent()) {
            filtro.filter(pedido);
            assertEquals(TRAZA, MDC.get("trace_id"));
            assertEquals(SPAN, MDC.get("span_id"));
        }

        filtro.filter(pedido, respuesta);

        assertNull(MDC.get("trace_id"));
        assertNull(MDC.get("span_id"));
    }
}
