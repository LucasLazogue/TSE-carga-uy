package uy.tse.cargauy.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatoTest {

    @Test
    fun kilos_conSeparadorDeMilesUruguayo() {
        assertEquals("18.000 kg", kilosConUnidad(18000))
        assertEquals("400 kg", kilosConUnidad(400))
    }

    @Test
    fun fechas_delCentralSeMuestranComoEnUruguay() {
        assertEquals("15/10/2026", fechaLegible("2026-10-15"))
        assertEquals("24/10 21:59", fechaHoraLegible("2026-10-24T21:59:46.156"))
    }

    @Test
    fun fechas_queNoSeEntiendenSeMuestranTalCual() {
        assertEquals("pronto", fechaLegible("pronto"))
    }

    @Test
    fun momentoDelEvento_soloLaHoraSiFueHoy() {
        val zona = ZoneId.of("America/Montevideo")
        val hoy = LocalDate.of(2026, 10, 25)
        val hoyALas = LocalDateTime.of(2026, 10, 25, 9, 5).atZone(zona).toInstant().toEpochMilli()
        val ayerALas = LocalDateTime.of(2026, 10, 24, 21, 59).atZone(zona).toInstant().toEpochMilli()

        assertEquals("09:05", momentoDelEvento(hoyALas, zona, hoy))
        assertEquals("24/10 21:59", momentoDelEvento(ayerALas, zona, hoy))
    }

    @Test
    fun detalle_juntaKilosYDescripcion() {
        assertEquals("400 kg · Deposito de Salto", detalleDelEvento(400, "Deposito de Salto"))
        assertEquals("400 kg", detalleDelEvento(400, null))
        assertEquals("Pinchadura", detalleDelEvento(null, "Pinchadura"))
        assertNull(detalleDelEvento(null, " "))
    }

    @Test
    fun cedula_conPuntosYGuionAntesDelVerificador() {
        assertEquals("5.555.555-5", cedulaLegible("55555555"))
        assertEquals("123.456-7", cedulaLegible("1234567"))
        assertEquals("abc", cedulaLegible("abc"))
    }

    @Test
    fun coordenadas_conCuatroDecimales() {
        assertEquals("-34.9011, -56.1645", coordenadas(-34.90114, -56.16449))
    }
}
