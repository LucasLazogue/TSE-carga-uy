package tse.cargauy.negocio.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class MetricasIngestaTest {

    private final SimpleMeterRegistry registro = new SimpleMeterRegistry();
    private final MetricasIngesta metricas = new MetricasIngesta();

    @BeforeEach
    void preparar() {
        metricas.registro = registro;
        metricas.crear();
    }

    @Test
    void cuentaLasPosicionesPorResultadoYMideLosTiempos() {
        long ahora = System.currentTimeMillis();
        metricas.procesada("REGISTRADO", System.nanoTime(), ahora - 200, Instant.ofEpochMilli(ahora - 60_000));
        metricas.procesada("REGISTRADO", System.nanoTime(), ahora - 100, Instant.ofEpochMilli(ahora - 30_000));
        metricas.procesada("DUPLICADO", System.nanoTime(), ahora, Instant.ofEpochMilli(ahora));

        assertEquals(2, registro.counter("ingesta.eventos", "resultado", "REGISTRADO").count());
        assertEquals(1, registro.counter("ingesta.eventos", "resultado", "DUPLICADO").count());
        assertEquals(3, registro.timer("ingesta.procesamiento").count());
        assertTrue(registro.timer("ingesta.espera").max(TimeUnit.MILLISECONDS) >= 200);
        assertTrue(registro.timer("ingesta.antiguedad").max(TimeUnit.SECONDS) >= 60);
    }

    // un mensaje mal formado no tiene hora de generacion, y uno con la hora del GPS adelantada no se mide
    @Test
    void sinHoraDeGeneracionOConLaHoraAdelantadaNoMideLaAntiguedad() {
        long ahora = System.currentTimeMillis();
        metricas.procesada("MAL_FORMADO", System.nanoTime(), ahora, null);
        metricas.procesada("REGISTRADO", System.nanoTime(), ahora, Instant.ofEpochMilli(ahora + 60_000));

        assertEquals(0, registro.timer("ingesta.antiguedad").count());
        assertEquals(2, registro.timer("ingesta.espera").count());
    }

    @Test
    void cuentaLosErroresPorTipo() {
        metricas.error("MAL_FORMADO");
        metricas.error("MongoTimeoutException");
        metricas.error("MongoTimeoutException");

        assertEquals(1, registro.counter("ingesta.errores", "tipo", "MAL_FORMADO").count());
        assertEquals(2, registro.counter("ingesta.errores", "tipo", "MongoTimeoutException").count());
    }
}
