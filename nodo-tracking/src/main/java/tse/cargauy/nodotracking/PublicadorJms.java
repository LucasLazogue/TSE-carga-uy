package tse.cargauy.nodotracking;

import java.util.List;

import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.Queue;

// publica en queue_tracking del broker del componente central (DA02), que esta en su propio contenedor: el nodo se
// conecta directo al broker, con TLS (R8), y no a una instancia de WildFly
public class PublicadorJms implements Publicador, AutoCloseable {

    private static final String COLA = "queue_tracking";

    // el contexto de la traza va en las propiedades del mensaje (traceparent), que lee el central al procesarlo
    private static final TextMapSetter<JMSProducer> PROPIEDADES = (productor, clave, valor) -> {
        if (productor != null) {
            productor.setProperty(clave, valor);
        }
    };

    private final Config config;
    private final Tracer tracer;
    private final TextMapPropagator propagador;
    private ActiveMQJMSConnectionFactory fabrica;
    // null = desconectado. volatile: /health lo lee desde otro hilo sin tomar el lock
    private volatile JMSContext principal;
    private Queue cola;

    public PublicadorJms(Config config) {
        this(config, OpenTelemetry.noop());
    }

    public PublicadorJms(Config config, OpenTelemetry openTelemetry) {
        this.config = config;
        this.tracer = openTelemetry.getTracer("nodo-tracking");
        this.propagador = openTelemetry.getPropagators().getTextMapPropagator();
    }

    // una traza por lote: cada mensaje lleva su contexto, y el procesamiento de cada posicion en el central es una
    // parte de esa traza
    @Override
    public void publicar(List<String> mensajes) throws PublicadorException {
        Span span = tracer.spanBuilder("publish " + COLA)
                .setSpanKind(SpanKind.PRODUCER)
                .setAttribute("messaging.system", "activemq")
                .setAttribute("messaging.destination.name", COLA)
                .setAttribute("messaging.operation.type", "send")
                .setAttribute("messaging.batch.message_count", mensajes.size())
                .startSpan();
        try (Scope alcance = span.makeCurrent(); JMSContext sesion = nuevaSesion()) {
            JMSProducer productor = sesion.createProducer();
            propagador.inject(Context.current(), productor, PROPIEDADES);
            for (String mensaje: mensajes) {
                productor.send(cola, mensaje);
            }
            sesion.commit();
        } catch (JMSRuntimeException e) {
            fallo(span, e);
            desconectar();
            throw new PublicadorException("No se pudo publicar en el broker", e);
        } catch (PublicadorException e) {
            fallo(span, e);
            throw e;
        } finally {
            span.end();
        }
    }

    private static void fallo(Span span, Exception e) {
        span.recordException(e);
        span.setStatus(StatusCode.ERROR);
    }

    // no intenta conectar: con el central caido un intento puede quedar colgado hasta el timeout de TCP, y /health
    // no responderia. la reconexion la hace el hilo que publica en cada reintento
    @Override
    public boolean disponible() {
        return principal != null;
    }

    private synchronized JMSContext nuevaSesion() throws PublicadorException {
        conectar();
        return principal.createContext(JMSContext.SESSION_TRANSACTED);
    }

    private synchronized void conectar() throws PublicadorException {
        if (principal != null) {
            return; // ya conectado
        }
        try {
            fabrica = new ActiveMQJMSConnectionFactory(config.jmsUrl());
            principal = fabrica.createContext(config.jmsUsuario(), config.jmsClave());
            // el cliente avisa cuando se cae la conexion: sin esto /health diria UP hasta que falle un envio
            principal.setExceptionListener(e -> desconectar());
            cola = principal.createQueue(COLA);
        } catch (JMSRuntimeException e) {
            desconectar();
            throw new PublicadorException("No se pudo conectar al broker", e);
        }
    }

    // se llama despues de un error: el proximo publicar vuelve a conectar
    private synchronized void desconectar() {
        if (principal != null) {
            try {
                principal.close();
            } catch (JMSRuntimeException ignorada) {
                // la conexion ya estaba rota: no hay nada mas que cerrar
            }
            principal = null;
        }
        if (fabrica != null) {
            fabrica.close();
            fabrica = null;
        }
    }

    @Override
    public void close() {
        desconectar();
    }
}