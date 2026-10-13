package tse.cargauy.nodotracking;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.context.Context;

// manda cada registro de java.util.logging como un log estructurado por OTLP (AC21). Si se escribe dentro de una
// traza, lleva su trace_id: es el identificador de correlacion con los logs del componente central
final class LogsOtel extends Handler {

    private static final AttributeKey<String> LOGGER = AttributeKey.stringKey("code.namespace");
    private static final AttributeKey<String> EXCEPCION = AttributeKey.stringKey("exception.stacktrace");

    private final Logger destino;
    private final SimpleFormatter mensaje = new SimpleFormatter();

    LogsOtel(OpenTelemetry openTelemetry) {
        destino = openTelemetry.getLogsBridge().get("nodo-tracking");
    }

    @Override
    public void publish(LogRecord registro) {
        if (!isLoggable(registro)) {
            return;
        }
        LogRecordBuilder log = destino.logRecordBuilder()
                .setTimestamp(registro.getInstant())
                .setSeverity(severidad(registro.getLevel()))
                .setSeverityText(registro.getLevel().getName())
                .setBody(mensaje.formatMessage(registro))
                .setAttribute(LOGGER, registro.getLoggerName())
                .setContext(Context.current());
        if (registro.getThrown() != null) {
            StringWriter traza = new StringWriter();
            registro.getThrown().printStackTrace(new PrintWriter(traza));
            log.setAttribute(EXCEPCION, traza.toString());
        }
        log.emit();
    }

    static Severity severidad(Level nivel) {
        int valor = nivel.intValue();
        if (valor >= Level.SEVERE.intValue()) {
            return Severity.ERROR;
        }
        if (valor >= Level.WARNING.intValue()) {
            return Severity.WARN;
        }
        if (valor >= Level.INFO.intValue()) {
            return Severity.INFO;
        }
        if (valor >= Level.FINE.intValue()) {
            return Severity.DEBUG;
        }
        return Severity.TRACE;
    }

    @Override
    public void flush() {
        // el SDK exporta en lotes y se vacia al cerrar
    }

    @Override
    public void close() {
        // lo cierra Observabilidad
    }
}
