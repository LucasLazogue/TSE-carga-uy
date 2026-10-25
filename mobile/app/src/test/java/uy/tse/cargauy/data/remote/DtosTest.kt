package uy.tse.cargauy.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DtosTest {

    // respuesta real de GET /api/movil/viaje: trae campos que la app no usa y omite los null (fechaInicio)
    @Test
    fun viajeMovil_seLeeDelJsonDelBackend() {
        val json = """
            {"guia":{"destinoLat":-30.9053,"destinoLon":-55.5508,"fecha":"2026-10-15","id":1,"idEmpresa":1,
            "idRegistradaPor":2,"idRubro":1,"idTipoCarga":2,"idViaje":1,"nombreEmpresa":"Transportes del Sur",
            "nombreRubro":"Agropecuario","nombreTipoCarga":"Granos","nroGuia":"G-000001","origenLat":-34.9011,
            "origenLon":-56.1645,"volumen":18000},"viaje":{"cedulaChofer":"55555555","estado":"ASIGNADO","id":1,
            "idChofer":2,"idGuia":1,"idVehiculo":1,"matricula":"STA1234","nroGuia":"G-000001"}}
        """.trimIndent()

        val dto = ApiClient.json.decodeFromString<ViajeMovilDto>(json)

        assertEquals(1L, dto.viaje.id)
        assertEquals("ASIGNADO", dto.viaje.estado)
        assertEquals("STA1234", dto.viaje.matricula)
        assertNull(dto.viaje.fechaInicio)
        assertEquals("G-000001", dto.guia.nroGuia)
        assertEquals(18000, dto.guia.volumen)
        assertEquals(-34.9011, dto.guia.origenLat!!, 0.0)
    }

    @Test
    fun token_seLeeConLosNombresDelEstandar() {
        val dto = ApiClient.json.decodeFromString<TokenRespuesta>(
            """{"access_token":"abc","token_type":"Bearer","expires_in":21600}"""
        )
        assertEquals("abc", dto.accessToken)
        assertEquals(21600L, dto.expiresIn)
    }

    // el login trae el refresh token; la renovacion no, y la app sigue usando el que tiene
    @Test
    fun token_conRefreshEnElLoginYSinElAlRenovar() {
        val login = ApiClient.json.decodeFromString<TokenRespuesta>(
            """{"access_token":"a","token_type":"Bearer","expires_in":900,"refresh_expires_in":604800,"refresh_token":"r"}"""
        )
        val renovacion = ApiClient.json.decodeFromString<TokenRespuesta>(
            """{"access_token":"b","token_type":"Bearer","expires_in":900,"refresh_expires_in":604800}"""
        )
        assertEquals("r", login.refreshToken)
        assertEquals(604800L, login.refreshExpiresIn)
        assertNull(renovacion.refreshToken)
    }

    @Test
    fun sync_sinViajeDejaViajeEnNull() {
        val dto = ApiClient.json.decodeFromString<SyncRespuestaDto>(
            """{"resultados":[{"idEvento":"e1","resultado":"CONFLICTO","mensaje":"El viaje 2 ya no esta asignado a este chofer."}]}"""
        )
        assertEquals("CONFLICTO", dto.resultados.single().resultado)
        assertNull(dto.viaje)
    }
}
