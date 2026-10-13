package tse.cargauy.nodotracking;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import jakarta.json.Json;

// GET /health, sin clave: lo consultan la PaaS y el monitoreo, que no conocen la clave de los GPS
public class HealthHandler implements HttpHandler {

    private final Publicador publicador;

    public HealthHandler(Publicador publicador) {
        this.publicador = publicador;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equals(exchange.getRequestMethod())) {
                Respuestas.json(exchange, 405, Respuestas.error("metodo no permitido"));
                return;
            }
            boolean arriba = publicador.disponible();
            Respuestas.json(exchange, arriba ? 200 : 503,
                    Json.createObjectBuilder().add("estado", arriba ? "UP" : "DOWN").build());
        } finally {
            exchange.close();
        }
    }
}
