package tse.cargauy.nodotracking;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;

import jakarta.json.Json;
import jakarta.json.JsonObject;

final class Respuestas {

    private Respuestas() {
    }

    static void json(HttpExchange exchange, int codigo, JsonObject cuerpo) throws IOException {
        byte[] bytes = cuerpo.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        // el largo va en bytes: con caracteres de mas de un byte el cliente cortaria la respuesta
        exchange.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream salida = exchange.getResponseBody()) {
            salida.write(bytes);
        }
    }

    static JsonObject error(String mensaje) {
        return Json.createObjectBuilder().add("error", mensaje).build();
    }
}
