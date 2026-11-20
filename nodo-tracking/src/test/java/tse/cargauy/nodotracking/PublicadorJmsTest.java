package tse.cargauy.nodotracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;

// el publicador contra un broker Artemis embebido (en memoria, sin TLS): publica el lote en queue_tracking en una sola
// transaccion, con el contexto de la traza en cada mensaje, y ante una falla avisa para que el generador reintente
class PublicadorJmsTest {

    private static final String URL = "vm://0";

    private final InMemorySpanExporter spans = InMemorySpanExporter.create();
    private EmbeddedActiveMQ broker;
    private OpenTelemetrySdk otel;

    @BeforeEach
    void arrancar() throws Exception {
        broker = new EmbeddedActiveMQ().setConfiguration(new ConfigurationImpl()
                .setPersistenceEnabled(false)
                .setSecurityEnabled(false)
                .addAcceptorConfiguration("en-memoria", URL));
        broker.start();
        otel = OpenTelemetrySdk.builder()
                .setTracerProvider(SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(spans)).build())
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
    }

    @AfterEach
    void parar() throws Exception {
        otel.close();
        if (broker != null) {
            broker.stop();
        }
    }

    private static Config config(String url) {
        return new Config("nodo-prueba", 0, url, "usuario", "clave", 500, 1000);
    }

    private static List<Message> recibir(int cantidad) {
        List<Message> recibidos = new ArrayList<>();
        try (ActiveMQJMSConnectionFactory fabrica = new ActiveMQJMSConnectionFactory(URL);
                JMSContext contexto = fabrica.createContext();
                JMSConsumer consumidor = contexto.createConsumer(contexto.createQueue("queue_tracking"))) {
            for (int i = 0; i < cantidad; i++) {
                Message mensaje = consumidor.receive(5000);
                if (mensaje != null) {
                    recibidos.add(mensaje);
                }
            }
        }
        return recibidos;
    }

    @Test
    void publicaElLoteConLaTrazaEnCadaMensaje() throws Exception {
        try (PublicadorJms publicador = new PublicadorJms(config(URL), otel)) {
            assertFalse(publicador.disponible(), "no conecta hasta el primer envio");

            publicador.publicar(List.of("{\"idEvento\":\"e-1\"}", "{\"idEvento\":\"e-2\"}"));

            assertTrue(publicador.disponible());
        }

        List<Message> recibidos = recibir(2);
        assertEquals(2, recibidos.size());
        assertEquals("{\"idEvento\":\"e-1\"}", ((TextMessage) recibidos.get(0)).getText());
        String traceparent = recibidos.get(0).getStringProperty("traceparent");
        assertNotNull(traceparent, "el central continua la traza del nodo");
        SpanData span = spans.getFinishedSpanItems().get(0);
        assertEquals("publish queue_tracking", span.getName());
        assertTrue(traceparent.contains(span.getTraceId()));
    }

    // con el broker caido: error para que el generador guarde el lote y reintente, y /health pasa a DOWN
    @Test
    void sinBrokerAvisaElFallo() throws Exception {
        broker.stop();
        broker = null;

        try (PublicadorJms publicador = new PublicadorJms(config(URL))) {
            PublicadorException e = assertThrows(PublicadorException.class, () -> publicador.publicar(List.of("{}")));

            assertTrue(e.getMessage().contains("broker"));
            assertFalse(publicador.disponible());
        }
    }

    @Test
    void siSeCaeDespuesReconectaEnElProximoEnvio() throws Exception {
        try (PublicadorJms publicador = new PublicadorJms(config(URL), otel)) {
            publicador.publicar(List.of("{\"idEvento\":\"e-1\"}"));
            broker.stop();

            assertThrows(PublicadorException.class, () -> publicador.publicar(List.of("{\"idEvento\":\"e-2\"}")));
            assertFalse(publicador.disponible());

            broker.start();
            publicador.publicar(List.of("{\"idEvento\":\"e-3\"}"));
            assertTrue(publicador.disponible());
        }
        assertTrue(spans.getFinishedSpanItems().stream().anyMatch(s -> !s.getEvents().isEmpty()), "el fallo queda en la traza");
    }
}
