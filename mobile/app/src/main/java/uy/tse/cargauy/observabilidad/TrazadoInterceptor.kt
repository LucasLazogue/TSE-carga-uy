package uy.tse.cargauy.observabilidad

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapSetter
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Un span por pedido al central, con el traceparent en los headers para que el central continúe la traza.
 * Va primero en el cliente: el span incluye la renovación del token si el central responde 401.
 */
class TrazadoInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val pedido = chain.request()
        val span = Telemetria.tracer.spanBuilder("${pedido.method} ${pedido.url.encodedPath}")
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute(METODO, pedido.method)
            // sin la query: puede llevar el code_challenge del login
            .setAttribute(URL, pedido.url.newBuilder().query(null).build().toString())
            .startSpan()
        try {
            span.makeCurrent().use {
                val conTraza = pedido.newBuilder()
                Telemetria.openTelemetry.propagators.textMapPropagator.inject(Context.current(), conTraza, HEADERS)
                val respuesta = chain.proceed(conTraza.build())
                span.setAttribute(ESTADO, respuesta.code.toLong())
                if (respuesta.code >= 500) span.setStatus(StatusCode.ERROR)
                return respuesta
            }
        } catch (e: IOException) {
            // sin red o el central no responde
            span.recordException(e)
            span.setStatus(StatusCode.ERROR)
            throw e
        } finally {
            span.end()
        }
    }

    private companion object {
        val METODO: AttributeKey<String> = AttributeKey.stringKey("http.request.method")
        val URL: AttributeKey<String> = AttributeKey.stringKey("url.full")
        val ESTADO: AttributeKey<Long> = AttributeKey.longKey("http.response.status_code")
        val HEADERS = TextMapSetter<Request.Builder> { pedido, clave, valor -> pedido?.header(clave, valor) }
    }
}
