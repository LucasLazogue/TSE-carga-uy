package uy.tse.cargauy.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtosTest {

    // un viaje de la respuesta real de GET /api/movil/viajes: trae campos que la app no usa y omite los null (fechaInicio)
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
    fun viajes_seLeeLaListaEnElOrdenDelCentral() {
        val json = """
            [{"guia":{"id":9001,"nroGuia":"G-009001","volumen":1000},
              "viaje":{"id":9001,"estado":"EN_CURSO","fechaInicio":"2026-10-24T21:59:46.156"}},
             {"guia":{"id":52,"nroGuia":"G-000052","volumen":12000},"viaje":{"id":52,"estado":"ASIGNADO"}}]
        """.trimIndent()

        val viajes = ApiClient.json.decodeFromString<List<ViajeMovilDto>>(json)

        assertEquals(listOf(9001L, 52L), viajes.map { it.viaje.id })
        assertEquals("2026-10-24T21:59:46.156", viajes[0].viaje.fechaInicio)
    }

    @Test
    fun sync_traeLosViajesComoQuedaron() {
        val dto = ApiClient.json.decodeFromString<SyncRespuestaDto>(
            """{"resultados":[{"idEvento":"e1","resultado":"APLICADO"}],
               "viajes":[{"guia":{"id":52},"viaje":{"id":52,"estado":"EN_CURSO"}}]}"""
        )
        assertEquals("EN_CURSO", dto.viajes.single().viaje.estado)
    }

    @Test
    fun sync_sinViajesDejaLaListaVacia() {
        val dto = ApiClient.json.decodeFromString<SyncRespuestaDto>(
            """{"resultados":[{"idEvento":"e1","resultado":"CONFLICTO","mensaje":"El viaje 2 ya no esta asignado a este chofer."}]}"""
        )
        assertEquals("CONFLICTO", dto.resultados.single().resultado)
        assertTrue(dto.viajes.isEmpty())
    }

    @Test
    fun evento_mandaDescripcionYKilosSoloSiEstan() {
        val conDatos = EventoSyncDto("e1", 2, "DESCARGA_PARCIAL", "2026-10-25T12:00:00Z", "Deposito de Salto", 400)
        val sinDatos = EventoSyncDto("e2", 2, "INICIO", "2026-10-25T12:00:00Z")

        val json = ApiClient.json.encodeToString(conDatos)
        assertTrue(json.contains("\"descripcion\":\"Deposito de Salto\""))
        assertTrue(json.contains("\"kilos\":400"))
        val vacio = ApiClient.json.encodeToString(sinDatos)
        assertFalse(vacio.contains("descripcion"))
        assertFalse(vacio.contains("kilos"))
    }
}
