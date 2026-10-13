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
import jakarta.json.JsonObject;

// tamano de la cola de tracking y cantidad de consumidores (AC22). El broker esta en su propio contenedor y no
// exporta metricas: se leen cada 10 s de su API de administracion (Jolokia), con un usuario que puede consultarla
@Singleton
@Startup
public class MetricasCola {

    private static final Logger LOGGER = Logger.getLogger(MetricasCola.class.getName());

    private static final String COLA = "queue_tracking";
    private static final String MBEAN = "org.apache.activemq.artemis:broker=\"carga-uy\",component=addresses,address=\""
            + COLA + "\",subcomponent=queues,routing-type=\"anycast\",queue=\"" + COLA + "\"";
    // -1 = no se pudo consultar; la metrica queda sin valor en lugar de mostrar un dato viejo
    private static final long DESCONOCIDO = -1;

    @Inject
    MeterRegistry registro;

    private final AtomicLong mensajes = new AtomicLong(DESCONOCIDO);
    private final AtomicLong consumidores = new AtomicLong(DESCONOCIDO);
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
    }

    @Schedule(second = "*/10", minute = "*", hour = "*", persistent = false)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    void consultar() {
        String pedido = Json.createObjectBuilder()
                .add("type", "read")
                .add("mbean", MBEAN)
                .add("attribute", Json.createArrayBuilder().add("MessageCount").add("ConsumerCount"))
                .build()
                .toString();
        try {
            HttpResponse<String> respuesta = cliente.send(HttpRequest.newBuilder(jolokia)
                    .timeout(Duration.ofSeconds(3))
                    .header("Authorization", autorizacion)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(pedido))
                    .build(), HttpResponse.BodyHandlers.ofString());
            JsonObject valor = Json.createReader(new StringReader(respuesta.body())).readObject().getJsonObject("value");
            mensajes.set(valor.getJsonNumber("MessageCount").longValue());
            consumidores.set(valor.getJsonNumber("ConsumerCount").longValue());
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            mensajes.set(DESCONOCIDO);
            consumidores.set(DESCONOCIDO);
            LOGGER.log(Level.FINE, "no se pudo consultar la cola en el broker", e);
        }
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
