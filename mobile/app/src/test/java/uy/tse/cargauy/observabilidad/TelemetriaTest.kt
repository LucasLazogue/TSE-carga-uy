package uy.tse.cargauy.observabilidad

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** El token de la Route de la app viaja en el header Authorization de cada envio de trazas, y solo si hay token. */
class TelemetriaTest {

    private lateinit var servidor: HttpServer
    private val recibidos = CopyOnWriteArrayList<Pair<String, String?>>()

    @Before
    fun levantarPlataforma() {
        servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/") { intercambio ->
            recibidos.add(intercambio.requestURI.path to intercambio.requestHeaders.getFirst("Authorization"))
            intercambio.sendResponseHeaders(200, -1)
            intercambio.close()
        }
        servidor.start()
    }

    @After
    fun bajarPlataforma() {
        servidor.stop(0)
    }

    private fun mandarUnSpan(token: String) {
        Telemetria.iniciar("http://127.0.0.1:${servidor.address.port}/", token)
        Telemetria.tracer.spanBuilder("prueba").startSpan().end()
        Telemetria.enviarPendientes()
    }

    @Test
    fun conTokenMandaElHeaderAuthorization() {
        mandarUnSpan("token-de-prueba")

        assertTrue("no llego ningun envio", recibidos.isNotEmpty())
        assertEquals("/v1/traces", recibidos[0].first)
        assertEquals("Bearer token-de-prueba", recibidos[0].second)
    }

    @Test
    fun sinTokenNoMandaHeader() {
        mandarUnSpan("")

        assertTrue("no llego ningun envio", recibidos.isNotEmpty())
        assertEquals(null, recibidos[0].second)
    }
}
