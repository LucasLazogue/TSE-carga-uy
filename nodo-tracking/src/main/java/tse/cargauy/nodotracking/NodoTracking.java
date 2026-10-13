package tse.cargauy.nodotracking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpServer;

// componente periferico del Sistema de Tracking: simula la flota y publica sus posiciones en queue_tracking
public class NodoTracking {

    // una linea por mensaje (fecha, nivel y texto) en lugar de las dos del formato por defecto. va antes del LOGGER
    // para que ya rija cuando se cree el primer handler
    private static final String FORMATO_LOG = "%1$tF %1$tT %4$s %5$s%6$s%n";

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", FORMATO_LOG);
    }

    private static final Logger LOGGER = Logger.getLogger(NodoTracking.class.getName());

    public static void main(String[] args) throws IOException {
        Config config = Config.desdeEntorno();
        ConfigFlota configFlota = ConfigFlota.desdeEntorno();
        Random azar = new Random();
        List<VehiculoSimulado> vehiculos = Flota.armar(configFlota, azar);
        // no conecta al arrancar: el nodo levanta aunque el central todavia no este listo
        PublicadorJms publicador = new PublicadorJms(config);
        GeneradorPosiciones generador = new GeneradorPosiciones(config, configFlota, vehiculos, publicador, azar, Instant.now());

        // un hilo genera y otro publica: si publicar se demora porque el central esta lento, la generacion no se atrasa.
        // el tercero es para el resumen periodico, asi tampoco espera a un envio lento
        ScheduledExecutorService reloj = Executors.newScheduledThreadPool(3);
        reloj.scheduleAtFixedRate(sinCortar(() -> generador.generar(Instant.now())),
                0, configFlota.intervalo().toMillis(), TimeUnit.MILLISECONDS);
        reloj.scheduleWithFixedDelay(sinCortar(() -> generador.publicarPendientes(Instant.now())),
                1, 1, TimeUnit.SECONDS);
        // un resumen cada tantos segundos en lugar de una linea por evento: el log no crece con el tamanio de la flota
        int logMetricasSeg = Config.numero("NODO_LOG_METRICAS_SEG", 60);
        if (logMetricasSeg > 0) {
            reloj.scheduleAtFixedRate(sinCortar(() -> LOGGER.info("nodo metricas=" + generador.metricas())),
                    logMetricasSeg, logMetricasSeg, TimeUnit.SECONDS);
        }

        HttpServer servidor = HttpServer.create(new InetSocketAddress(config.puerto()), 0);
        servidor.createContext("/health", new HealthHandler(publicador));
        servidor.createContext("/metricas", new MetricasHandler(generador));
        servidor.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            reloj.shutdownNow();
            // en una parada planificada se intenta publicar lo pendiente antes de cerrar, para no perderlo
            generador.publicarPendientes(Instant.now());
            // System.out y no LOGGER: java.util.logging cierra sus handlers en su propio hook y este mensaje se perderia
            System.out.printf("%1$tF %1$tT INFO nodo detenido metricas=%2$s%n", new Date(), generador.metricas());
            servidor.stop(1);
            publicador.close();
        }));
        LOGGER.info("Nodo " + config.nodoId() + " simulando " + vehiculos.size() + " vehiculos cada "
                + configFlota.intervalo().toSeconds() + " s; metricas en el puerto " + config.puerto());
    }

    // si una tarea programada tira una excepcion, el executor no la vuelve a correr nunca y no avisa:
    // se loguea y se sigue
    private static Runnable sinCortar(Runnable tarea) {
        return () -> {
            try {
                tarea.run();
            } catch (RuntimeException e) {
                LOGGER.log(Level.SEVERE, "nodo error inesperado", e);
            }
        };
    }
}
