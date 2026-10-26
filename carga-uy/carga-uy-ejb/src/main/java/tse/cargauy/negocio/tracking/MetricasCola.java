package tse.cargauy.negocio.tracking;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

// tamano de la cola de tracking y cantidad de consumidores (AC22). El broker esta en su propio contenedor y no
// exporta metricas: se leen cada 10 s de su API de administracion (Jolokia), con un usuario que puede consultarla
@Singleton
@Startup
public class MetricasCola {

    private static final Logger LOGGER = Logger.getLogger(MetricasCola.class.getName());

    private static final String COLA = "queue_tracking";
    // los mensajes que agotaron sus reentregas (DA02): tiene que quedar en 0
    private static final String MUERTOS = "DLQ";
    // -1 = no se pudo consultar; la metrica queda sin valor en lugar de mostrar un dato viejo
    private static final long DESCONOCIDO = -1;

    @Inject
    MeterRegistry registro;

    private final AtomicLong mensajes = new AtomicLong(DESCONOCIDO);
    private final AtomicLong consumidores = new AtomicLong(DESCONOCIDO);
    private final AtomicLong muertos = new AtomicLong(DESCONOCIDO);
    private HttpClient cliente;
    private URI jolokia;
    private String autorizacion;

    @PostConstruct
    void iniciar() {
        cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        jolokia = URI.create(entorno("ARTEMIS_CONSOLA", "http://artemis:8161") + "/console/jolokia/");
        String credenciales = entorno("ARTEMIS_CONSOLA_USUARIO", "artemis") + ":" + entorno("ARTEMIS_CONSOLA_CLAVE", "artemis");
        autorizacion = "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
        Gauge.builder("cola.mensajes", mensajes, MetricasCola::valor)
                .description("Mensajes esperando en la cola")
                .tag("cola", COLA)
                .register(registro);
        Gauge.builder("cola.consumidores", consumidores, MetricasCola::valor)
                .description("Consumidores conectados a la cola; cada instancia del central abre varios")
                .tag("cola", COLA)
                .register(registro);
        Gauge.builder("cola.mensajes", muertos, MetricasCola::valor)
                .description("Mensajes esperando en la cola")
                .tag("cola", MUERTOS)
                .register(registro);
    }

    @Schedule(second = "*/10", minute = "*", hour = "*", persistent = false)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    void consultar() {
        // las dos colas en un solo pedido (bulk de Jolokia): la respuesta es una lista en el mismo orden
        String pedido = Json.createArrayBuilder()
                .add(lectura(COLA, Json.createArrayBuilder().add("MessageCount").add("ConsumerCount")))
                .add(lectura(MUERTOS, Json.createArrayBuilder().add("MessageCount")))
                .build()
                .toString();
        try {
            HttpResponse<String> respuesta = cliente.send(HttpRequest.newBuilder(jolokia)
                    .timeout(Duration.ofSeconds(3))
                    .header("Authorization", autorizacion)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(pedido))
                    .build(), HttpResponse.BodyHandlers.ofString());
            JsonArray valores = Json.createReader(new StringReader(respuesta.body())).readArray();
            JsonObject cola = valores.getJsonObject(0).getJsonObject("value");
            mensajes.set(cola.getJsonNumber("MessageCount").longValue());
            consumidores.set(cola.getJsonNumber("ConsumerCount").longValue());
            muertos.set(valores.getJsonObject(1).getJsonObject("value").getJsonNumber("MessageCount").longValue());
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            mensajes.set(DESCONOCIDO);
            consumidores.set(DESCONOCIDO);
            muertos.set(DESCONOCIDO);
            LOGGER.log(Level.FINE, "no se pudo consultar la cola en el broker", e);
        }
    }

    private static JsonObject lectura(String cola, JsonArrayBuilder atributos) {
        return Json.createObjectBuilder()
                .add("type", "read")
                .add("mbean", "org.apache.activemq.artemis:broker=\"carga-uy\",component=addresses,address=\"" + cola
                        + "\",subcomponent=queues,routing-type=\"anycast\",queue=\"" + cola + "\"")
                .add("attribute", atributos)
                .build();
    }

    private static double valor(AtomicLong medida) {
        long v = medida.get();
        return v == DESCONOCIDO ? Double.NaN : v;
    }

    private static String entorno(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }
}
