package tse.cargauy.negocio.tracking;

import java.io.StringReader;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

import org.jboss.logging.MDC;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import tse.cargauy.dtos.EventoPosicionDto;

// formato del mensaje (JSON en un TextMessage):
// {"idEvento":"...","nodo":"...","matricula":"...","latitud":-34.9,"longitud":-56.1,"timestampGeneracion":"2026-10-06T12:00:00Z"}
// el nodo agrega la propiedad traceparent con el contexto de su traza (W3C), para que la del central la continue
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/queue_tracking"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue")
})
public class IngestaTrackingMDB implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(IngestaTrackingMDB.class.getName());

    private static final String COLA = "queue_tracking";

    // WildFly no traza JMS: el contexto de la traza del nodo se lee de las propiedades del mensaje
    private static final TextMapGetter<Message> PROPIEDADES = new TextMapGetter<>() {
        @Override
        @SuppressWarnings("unchecked")
        public Iterable<String> keys(Message mensaje) {
            try {
                return Collections.list((Enumeration<String>) mensaje.getPropertyNames());
            } catch (JMSException e) {
                return List.of();
            }
        }

        @Override
        public String get(Message mensaje, String clave) {
            try {
                return mensaje == null ? null : mensaje.getStringProperty(clave);
            } catch (JMSException e) {
                return null;
            }
        }
    };

    @EJB
    IngestaTrackingEJBLocal ingesta;

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    Tracer tracer;

    @Inject
    MetricasIngesta metricas;

    @Override
    public void onMessage(Message message) {
        long inicio = System.nanoTime();
        Context delNodo = openTelemetry.getPropagators().getTextMapPropagator().extract(Context.root(), message, PROPIEDADES);
        Span span = tracer.spanBuilder("process " + COLA)
                .setParent(delNodo)
                .setSpanKind(SpanKind.CONSUMER)
                .setAttribute("messaging.system", "activemq")
                .setAttribute("messaging.destination.name", COLA)
                .setAttribute("messaging.operation.type", "process")
                .startSpan();
        // el identificador de correlacion de los logs es el de la traza: el mismo en el nodo y en el central
        MDC.put("trace_id", span.getSpanContext().getTraceId());
        MDC.put("span_id", span.getSpanContext().getSpanId());
        try (Scope alcance = span.makeCurrent()) {
            procesar(message, inicio, span);
        } catch (RuntimeException e) {
            // si falla otra cosa (por ejemplo la base) la excepcion sigue de largo y el broker reentrega el mensaje
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            metricas.error(e.getClass().getSimpleName());
            metricas.procesada("ERROR", inicio, publicada(message), null);
            throw e;
        } finally {
            span.end();
            MDC.remove("trace_id");
            MDC.remove("span_id");
            MDC.remove("id_evento");
        }
    }

    private void procesar(Message message, long inicio, Span span) {
        EventoPosicionDto evento;
        try {
            evento = leer(message);
        } catch (JMSException | JsonException | ClassCastException | NullPointerException | DateTimeParseException e) {
            // un mensaje mal formado no se arregla reintentando: se descarta para no trancar la cola
            LOGGER.warning("ingesta resultado=MAL_FORMADO error=" + e);
            span.setAttribute("ingesta.resultado", "MAL_FORMADO");
            metricas.error("MAL_FORMADO");
            metricas.procesada("MAL_FORMADO", inicio, publicada(message), null);
            return;
        }
        MDC.put("id_evento", evento.getIdEvento());
        span.setAttribute("ingesta.id_evento", evento.getIdEvento());
        span.setAttribute("ingesta.nodo", evento.getNodo());
        span.setAttribute("ingesta.matricula", evento.getMatricula());
        ResultadoIngesta resultado = ingesta.procesar(evento);
        span.setAttribute("ingesta.resultado", resultado.name());
        if (resultado == ResultadoIngesta.NODO_DESCONOCIDO || resultado == ResultadoIngesta.NODO_NO_HABILITADO) {
            metricas.error(resultado.name());
        }
        metricas.procesada(resultado.name(), inicio, publicada(message), evento.getTimestampGeneracion());
        LOGGER.info("ingesta resultado=" + resultado + " evento=" + evento.getIdEvento()
                + " nodo=" + evento.getNodo() + " matricula=" + evento.getMatricula());
    }

    private static long publicada(Message message) {
        try {
            return message.getJMSTimestamp();
        } catch (JMSException e) {
            return 0;
        }
    }

    private static EventoPosicionDto leer(Message message) throws JMSException {
        if (!(message instanceof TextMessage texto)) {
            throw new JsonException("el mensaje no es de texto");
        }
        JsonObject json = Json.createReader(new StringReader(texto.getText())).readObject();
        EventoPosicionDto evento = new EventoPosicionDto();
        evento.setIdEvento(json.getString("idEvento"));
        evento.setNodo(json.getString("nodo"));
        evento.setMatricula(json.getString("matricula"));
        evento.setLatitud(json.getJsonNumber("latitud").doubleValue());
        evento.setLongitud(json.getJsonNumber("longitud").doubleValue());
        evento.setTimestampGeneracion(Instant.parse(json.getString("timestampGeneracion")));
        return evento;
    }
}
