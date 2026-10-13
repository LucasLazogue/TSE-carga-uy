package tse.cargauy.web.ws;

import java.util.concurrent.TimeUnit;

import org.jboss.logging.MDC;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

// Para cada pedido a la API: el identificador de correlacion en los logs (la traza que abre WildFly) y el tiempo de
// respuesta por operacion, con histograma para el percentil 95 que pide la prueba de performance (AC09)
@Provider
@Priority(Priorities.USER)
public class ObservabilidadFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String INICIO = ObservabilidadFilter.class.getName() + ".inicio";

    @Inject
    MeterRegistry registro;

    @Context
    ResourceInfo recurso;

    @Override
    public void filter(ContainerRequestContext pedido) {
        pedido.setProperty(INICIO, System.nanoTime());
        SpanContext traza = Span.current().getSpanContext();
        if (traza.isValid()) {
            MDC.put("trace_id", traza.getTraceId());
            MDC.put("span_id", traza.getSpanId());
        }
    }

    @Override
    public void filter(ContainerRequestContext pedido, ContainerResponseContext respuesta) {
        try {
            Object inicio = pedido.getProperty(INICIO);
            if (inicio instanceof Long nanos) {
                Timer.builder("http.servidor")
                        .description("Tiempo de respuesta de la API REST")
                        .tag("metodo", pedido.getMethod())
                        .tag("operacion", operacion())
                        .tag("estado", String.valueOf(respuesta.getStatus()))
                        .publishPercentileHistogram()
                        .register(registro)
                        .record(System.nanoTime() - nanos, TimeUnit.NANOSECONDS);
            }
        } finally {
            MDC.remove("trace_id");
            MDC.remove("span_id");
        }
    }

    // la clase y el metodo del recurso y no la URL, que tiene identificadores y daria una serie por cada uno
    private String operacion() {
        if (recurso == null || recurso.getResourceMethod() == null) {
            return "desconocida";
        }
        return recurso.getResourceClass().getSimpleName() + "." + recurso.getResourceMethod().getName();
    }
}
