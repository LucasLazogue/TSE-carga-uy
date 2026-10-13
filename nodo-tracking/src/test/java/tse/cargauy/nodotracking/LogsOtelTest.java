package tse.cargauy.nodotracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

class LogsOtelTest {

    private final InMemoryLogRecordExporter exportados = InMemoryLogRecordExporter.create();
    private final OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
            .setLoggerProvider(SdkLoggerProvider.builder().addLogRecordProcessor(SimpleLogRecordProcessor.create(exportados)).build())
            .setTracerProvider(SdkTracerProvider.builder().build())
            .build();

    // el trace_id es el identificador de correlacion con los logs del componente central
    @Test
    void unLogDentroDeUnaTrazaLlevaSuIdentificador() {
        Span span = sdk.getTracer("prueba").spanBuilder("publish queue_tracking").startSpan();
        try (Scope alcance = span.makeCurrent()) {
            new LogsOtel(sdk).publish(registro(Level.WARNING, "nodo resultado={0}", "BROKER_NO_DISPONIBLE"));
        } finally {
            span.end();
        }

        LogRecordData log = exportados.getFinishedLogRecordItems().get(0);
        assertEquals("nodo resultado=BROKER_NO_DISPONIBLE", log.getBodyValue().asString());
        assertEquals(Severity.WARN, log.getSeverity());
        assertEquals(span.getSpanContext().getTraceId(), log.getSpanContext().getTraceId());
    }

    @Test
    void unLogFueraDeUnaTrazaNoTieneIdentificador() {
        new LogsOtel(sdk).publish(registro(Level.INFO, "nodo metricas", null));

        assertFalse(exportados.getFinishedLogRecordItems().get(0).getSpanContext().isValid());
    }

    @Test
    void laExcepcionVaComoAtributo() {
        LogRecord registro = registro(Level.SEVERE, "nodo error inesperado", null);
        registro.setThrown(new IllegalStateException("falla"));

        new LogsOtel(sdk).publish(registro);

        String traza = exportados.getFinishedLogRecordItems().get(0).getAttributes().asMap().entrySet().stream()
                .filter(e -> e.getKey().getKey().equals("exception.stacktrace"))
                .map(e -> (String) e.getValue()).findFirst().orElse("");
        assertTrue(traza.contains("IllegalStateException: falla"));
    }

    @ParameterizedTest
    @CsvSource({ "SEVERE, ERROR", "WARNING, WARN", "INFO, INFO", "CONFIG, DEBUG", "FINE, DEBUG", "FINEST, TRACE" })
    void traduceLosNivelesDeJavaUtilLogging(String nivel, Severity esperada) {
        assertEquals(esperada, LogsOtel.severidad(Level.parse(nivel)));
    }

    private static LogRecord registro(Level nivel, String mensaje, String parametro) {
        LogRecord registro = new LogRecord(nivel, mensaje);
        registro.setLoggerName("tse.cargauy.nodotracking.NodoTracking");
        if (parametro != null) {
            registro.setParameters(new Object[] { parametro });
        }
        return registro;
    }
}
