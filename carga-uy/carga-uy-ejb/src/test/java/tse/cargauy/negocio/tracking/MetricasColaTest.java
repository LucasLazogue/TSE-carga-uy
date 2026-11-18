package tse.cargauy.negocio.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

// contra un Jolokia falso: lo que importa es que lea la cola y que, si no puede, lo diga una sola vez en el log
class MetricasColaTest {

    private static final String RESPUESTA = "[{\"value\":{\"MessageCount\":3,\"ConsumerCount\":15}},{\"value\":{\"MessageCount\":0}}]";

    private HttpServer servidor;
    private volatile int estado = 200;
    private final List<LogRecord> avisos = new ArrayList<>();
    private final Handler capturador = new Handler() {
        @Override
        public void publish(LogRecord registro) {
            if (registro.getLevel().intValue() >= Level.INFO.intValue()) {
                avisos.add(registro);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };
    private MetricasCola metricas;

    @BeforeEach
    void preparar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/console/jolokia/", intercambio -> {
            byte[] cuerpo = estado == 200 ? RESPUESTA.getBytes(StandardCharsets.UTF_8) : new byte[0];
            intercambio.sendResponseHeaders(estado, cuerpo.length == 0 ? -1 : cuerpo.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        servidor.start();
        metricas = new MetricasCola();
        metricas.cliente = HttpClient.newHttpClient();
        metricas.jolokia = URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/console/jolokia/");
        metricas.autorizacion = "Basic prueba";
        Logger.getLogger(MetricasCola.class.getName()).addHandler(capturador);
    }

    @AfterEach
    void cerrar() {
        Logger.getLogger(MetricasCola.class.getName()).removeHandler(capturador);
        servidor.stop(0);
    }

    @Test
    void leeLaColaYLosMensajesMuertos() {
        metricas.consultar();

        assertEquals(3, metricas.mensajes());
        assertEquals(15, metricas.consumidores());
        assertEquals(0, metricas.muertos());
        assertEquals(0, avisos.size());
    }

    @Test
    void siElBrokerRechazaAvisaUnaSolaVezYAlVolverLoDice() {
        estado = 403;
        metricas.consultar();
        metricas.consultar();
        metricas.consultar();

        assertEquals(-1, metricas.mensajes());
        assertEquals(-1, metricas.consumidores());
        assertEquals(1, avisos.size());
        assertEquals(Level.WARNING, avisos.get(0).getLevel());

        estado = 200;
        metricas.consultar();

        assertEquals(3, metricas.mensajes());
        assertEquals(2, avisos.size());
        assertEquals(Level.INFO, avisos.get(1).getLevel());
    }
}
