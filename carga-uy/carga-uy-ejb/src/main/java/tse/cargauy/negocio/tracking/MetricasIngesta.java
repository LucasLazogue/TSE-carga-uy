package tse.cargauy.negocio.tracking;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

// metricas de la ingesta (AC22): eventos procesados por resultado, errores por tipo y tiempos. WildFly las exporta
// por OTLP a la Plataforma de Observabilidad junto con las suyas
@ApplicationScoped
public class MetricasIngesta {

    @Inject
    MeterRegistry registro;

    private Timer procesamiento;
    private Timer espera;
    private Timer antiguedad;

    @PostConstruct
    void crear() {
        // con histograma, para calcular percentiles en el informe de performance
        procesamiento = Timer.builder("ingesta.procesamiento")
                .description("Lo que tarda el componente central en procesar una posicion")
                .publishPercentileHistogram()
                .register(registro);
        espera = Timer.builder("ingesta.espera")
                .description("Lo que espero la posicion en la cola desde que el nodo la publico")
                .publishPercentileHistogram()
                .register(registro);
        // incluye lo que la posicion estuvo retenida en el nodo, por ejemplo sin cobertura
        antiguedad = Timer.builder("ingesta.antiguedad")
                .description("Lo que paso desde que se genero la posicion hasta que se termino de procesar")
                .publishPercentileHistogram()
                .register(registro);
    }

    // publicada: cuando el nodo la mando al broker (JMSTimestamp); generada: la hora del GPS. Pueden faltar si el
    // mensaje no se pudo leer
    void procesada(String resultado, long inicioNanos, long publicada, Instant generada) {
        registro.counter("ingesta.eventos", "resultado", resultado).increment();
        procesamiento.record(System.nanoTime() - inicioNanos, TimeUnit.NANOSECONDS);
        long ahora = System.currentTimeMillis();
        if (publicada > 0 && ahora >= publicada) {
            espera.record(ahora - publicada, TimeUnit.MILLISECONDS);
        }
        if (generada != null && !generada.isAfter(Instant.ofEpochMilli(ahora))) {
            antiguedad.record(Duration.between(generada, Instant.ofEpochMilli(ahora)));
        }
    }

    void error(String tipo) {
        registro.counter("ingesta.errores", "tipo", tipo).increment();
    }
}
