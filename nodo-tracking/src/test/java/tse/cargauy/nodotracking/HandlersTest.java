package tse.cargauy.nodotracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import jakarta.json.Json;
import jakarta.json.JsonObject;

// los endpoints HTTP del nodo, contra un servidor real en un puerto libre
class HandlersTest {

    private final HttpClient cliente = HttpClient.newHttpClient();
    private HttpServer servidor;
    private String base;
    private boolean brokerDisponible;

    @BeforeEach
    void arrancar() throws IOException {
        Publicador publicador = new Publicador() {
            @Override
            public void publicar(List<String> mensajes) {
            }

            @Override
            public boolean disponible() {
                return brokerDisponible;
            }
        };
        Config config = new Config("nodo-prueba", 0, "vm://0", "u", "c", 500, 1000);
        ConfigFlota flota = new ConfigFlota(null, 0, Duration.ofSeconds(10), 1, 0, false, null, null);
        GeneradorPosiciones generador = new GeneradorPosiciones(config, flota,
                Flota.generar(2, 1, new Random(1)), publicador, new Random(1), Instant.now());
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/health", new HealthHandler(publicador));
        servidor.createContext("/metricas", new MetricasHandler(generador));
        servidor.start();
        base = "http://localhost:" + servidor.getAddress().getPort();
    }

    @AfterEach
    void parar() {
        servidor.stop(0);
    }

    private HttpResponse<String> get(String ruta) throws IOException, InterruptedException {
        return cliente.send(HttpRequest.newBuilder(URI.create(base + ruta)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String ruta) throws IOException, InterruptedException {
        return cliente.send(HttpRequest.newBuilder(URI.create(base + ruta)).POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject json(HttpResponse<String> respuesta) {
        return Json.createReader(new StringReader(respuesta.body())).readObject();
    }

    // /health responde segun la conexion al broker, sin intentar conectar
    @Test
    void healthSegunElBroker() throws Exception {
        brokerDisponible = true;
        HttpResponse<String> arriba = get("/health");
        brokerDisponible = false;
        HttpResponse<String> abajo = get("/health");

        assertEquals(200, arriba.statusCode());
        assertEquals("UP", json(arriba).getString("estado"));
        assertEquals("application/json", arriba.headers().firstValue("Content-Type").orElse(""));
        assertEquals(503, abajo.statusCode());
        assertEquals("DOWN", json(abajo).getString("estado"));
    }

    @Test
    void metricasDelGenerador() throws Exception {
        HttpResponse<String> respuesta = get("/metricas");

        assertEquals(200, respuesta.statusCode());
        assertEquals(2, json(respuesta).getInt("vehiculos"));
        assertTrue(json(respuesta).containsKey("publicados"));
    }

    @Test
    void soloGet() throws Exception {
        HttpResponse<String> health = post("/health");
        HttpResponse<String> metricas = post("/metricas");

        assertEquals(405, health.statusCode());
        assertEquals("metodo no permitido", json(health).getString("error"));
        assertEquals(405, metricas.statusCode());
    }
}
