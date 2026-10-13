package tse.cargauy.nodotracking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;

class ObservabilidadTest {

    private static final Instant INICIO = Instant.parse("2026-10-08T13:00:00Z");

    @Test
    void exportaLasMismasMetricasQueMuestraElNodo() {
        InMemoryMetricReader lector = InMemoryMetricReader.create();
        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(lector).build())
                .build();
        GeneradorPosiciones generador = generador(3, 2);
        generador.generar(INICIO);
        generador.publicarPendientes(INICIO.plusSeconds(1));
        generador.generar(INICIO.plusSeconds(10));

        new Observabilidad(sdk).medir(generador);
        Map<String, Long> metricas = valores(lector.collectAllMetrics());

        assertEquals(6, metricas.get("nodo.eventos.generados"));
        assertEquals(3, metricas.get("nodo.eventos.publicados"));
        assertEquals(3, metricas.get("nodo.eventos.pendientes"));
        assertEquals(0, metricas.get("nodo.publicacion.reintentos"));
    }

    private static Map<String, Long> valores(Iterable<MetricData> datos) {
        Map<String, Long> valores = new HashMap<>();
        for (MetricData metrica : datos) {
            LongPointData punto = (LongPointData) metrica.getData().getPoints().iterator().next();
            valores.put(metrica.getName(), punto.getValue());
        }
        return valores;
    }

    private static GeneradorPosiciones generador(int vehiculos, int maxLote) {
        Config config = new Config("nodo-test", 0, "no-se-usa", "u", "c", maxLote);
        Ruta ruta = new Ruta(new Punto(-34.9011, -56.1645), new Punto(-30.9053, -55.5508));
        List<VehiculoSimulado> lista = new ArrayList<>();
        for (int i = 0; i < vehiculos; i++) {
            lista.add(new VehiculoSimulado("V" + i, ruta, 80, 0));
        }
        ConfigFlota flota = new ConfigFlota(null, 0, Duration.ofSeconds(10), 1, 0, false, null, null);
        return new GeneradorPosiciones(config, flota, lista, new GeneradorPosicionesTest.PublicadorFalso(), new Random(1), INICIO);
    }
}
