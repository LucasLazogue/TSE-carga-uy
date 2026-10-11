package uy.tse.cargauy.data.remote

import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ErroresTest {

    private fun httpError(codigo: Int, cuerpo: String) =
        HttpException(Response.error<Any>(codigo, cuerpo.toResponseBody("application/json".toMediaType())))

    @Test
    fun usaElMensajeQueMandaElBackend() {
        val e = httpError(401, """{"codigo":4002,"mensaje":"La solicitud de ingreso no es valida o expiro."}""")
        assertEquals("La solicitud de ingreso no es valida o expiro.", mensajeDeError(e))
    }

    @Test
    fun sinCuerpoReconocibleMuestraElCodigoHttp() {
        assertEquals("El servidor respondió con un error (502).", mensajeDeError(httpError(502, "<html>Bad Gateway</html>")))
    }

    @Test
    fun sinConexionLoDice() {
        assertEquals(
            "No se pudo conectar con el servidor. Revisá la conexión y la URL.",
            mensajeDeError(IOException("timeout"))
        )
    }
}
