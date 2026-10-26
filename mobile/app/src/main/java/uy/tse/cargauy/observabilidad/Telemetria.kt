package uy.tse.cargauy.observabilidad

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import java.util.concurrent.TimeUnit

/**
 * Trazado distribuido desde la app (punto 9 de la letra, AC23): cada pedido al central abre un span y le pasa la traza
 * en el header traceparent, así el central y los nodos de balanzas siguen la misma traza que empezó en el celular.
 * Los spans van por OTLP a la Plataforma de Observabilidad. Sin su URL no se exportan, pero el traceparent se manda
 * igual: el central y los nodos quedan en la misma traza aunque falte el tramo del celular.
 */
object Telemetria {

    private val SERVICIO = AttributeKey.stringKey("service.name")

    @Volatile
    private var proveedor: SdkTracerProvider? = null

    @Volatile
    var openTelemetry: OpenTelemetry = OpenTelemetry.noop()
        private set

    val tracer: Tracer get() = openTelemetry.getTracer("carga-uy-movil")

    /** Una vez, al crear la app. urlPlataforma: la de OTLP por HTTP, por ejemplo http://IP:4318 */
    fun iniciar(urlPlataforma: String) {
        val constructor = SdkTracerProvider.builder()
            .setResource(Resource.getDefault().merge(Resource.create(Attributes.of(SERVICIO, "carga-uy-movil"))))
        if (urlPlataforma.isNotBlank()) {
            val exportador = OtlpHttpSpanExporter.builder()
                .setEndpoint(urlPlataforma.trimEnd('/') + "/v1/traces")
                .build()
            // en lotes y en otro hilo: exportar no demora los pedidos al central
            constructor.addSpanProcessor(BatchSpanProcessor.builder(exportador).build())
        }
        val nuevo = constructor.build()
        proveedor = nuevo
        openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(nuevo)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build()
    }

    /** El worker de sincronización puede terminar antes del próximo lote: manda lo pendiente al terminar. */
    fun enviarPendientes() {
        proveedor?.forceFlush()?.join(5, TimeUnit.SECONDS)
    }
}
