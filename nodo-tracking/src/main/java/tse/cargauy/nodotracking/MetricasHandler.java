package tse.cargauy.nodotracking;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

// GET /metricas: cuanto genero y publico el nodo. sirve para comprobar "sin perdida" en la prueba de carga:
// generados tiene que coincidir con las filas nuevas en eventoposicion
public class MetricasHandler implements HttpHandler {

    private final GeneradorPosiciones generador;

    public MetricasHandler(GeneradorPosiciones generador) {
        this.generador = generador;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equals(exchange.getRequestMethod())) {
                Respuestas.json(exchange, 405, Respuestas.error("metodo no permitido"));
                return;
            }
            Respuestas.json(exchange, 200, generador.metricas());
        } finally {
            exchange.close();
        }
    }
}
