package tse.cargauy.nodotracking;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import jakarta.json.JsonObject;

// trazas, metricas y logs del nodo hacia la Plataforma de Observabilidad, por OTLP (AC21 a AC23). Se configura con
// las variables estandar de OpenTelemetry (OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_TRACES_SAMPLER...); con
// OTEL_SDK_DISABLED=true el nodo funciona igual, sin exportar nada
final class Observabilidad {

    private final OpenTelemetrySdk sdk;

    Observabilidad(OpenTelemetrySdk sdk) {
        this.sdk = sdk;
    }

    static Observabilidad iniciar() {
        // valores por defecto: las variables de entorno los reemplazan
        OpenTelemetrySdk sdk = AutoConfiguredOpenTelemetrySdk.builder()
                .addPropertiesSupplier(() -> Map.of(
                        "otel.service.name", "nodo-tracking",
                        "otel.exporter.otlp.protocol", "http/protobuf"))
                .build()
                .getOpenTelemetrySdk();
        return new Observabilidad(sdk);
    }

    OpenTelemetry openTelemetry() {
        return sdk;
    }

    // las mismas metricas de /metricas, para verlas en la plataforma junto a las del central
    void medir(GeneradorPosiciones generador) {
        Meter medidor = sdk.getMeter("nodo-tracking");
        ObservableLongMeasurement generados = medidor.counterBuilder("nodo.eventos.generados")
                .setDescription("Posiciones generadas por la flota simulada").buildObserver();
        ObservableLongMeasurement duplicados = medidor.counterBuilder("nodo.eventos.duplicados")
                .setDescription("Posiciones publicadas dos veces a proposito").buildObserver();
        ObservableLongMeasurement publicados = medidor.counterBuilder("nodo.eventos.publicados")
                .setDescription("Mensajes publicados en el broker").buildObserver();
        ObservableLongMeasurement reintentos = medidor.counterBuilder("nodo.publicacion.reintentos")
                .setDescription("Publicaciones que fallaron porque el broker no respondio").buildObserver();
        ObservableLongMeasurement pendientes = medidor.gaugeBuilder("nodo.eventos.pendientes").ofLongs()
                .setDescription("Posiciones esperando para publicarse").buildObserver();
        ObservableLongMeasurement retenidos = medidor.gaugeBuilder("nodo.eventos.retenidos").ofLongs()
                .setDescription("Posiciones acumuladas mientras el vehiculo esta sin cobertura").buildObserver();
        medidor.batchCallback(() -> {
            JsonObject m = generador.metricas();
            generados.record(m.getJsonNumber("generados").longValue());
            duplicados.record(m.getJsonNumber("duplicados").longValue());
            publicados.record(m.getJsonNumber("publicados").longValue());
            reintentos.record(m.getJsonNumber("reintentos").longValue());
            pendientes.record(m.getJsonNumber("pendientes").longValue());
            retenidos.record(m.getJsonNumber("retenidosSinCobertura").longValue());
        }, generados, duplicados, publicados, reintentos, pendientes, retenidos);
    }

    // exporta lo que quedo pendiente antes de cerrar
    void cerrar() {
        sdk.shutdown().join(5, TimeUnit.SECONDS);
    }
}
