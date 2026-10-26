package uy.tse.cargauy.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

import uy.tse.cargauy.data.local.ViajeLocal

class AccionesTest {

    private val hoy = LocalDate.of(2026, 10, 25)

    private fun viaje(estado: String, fechaGuia: String = "2026-10-25") = ViajeLocal(
        idViaje = 1, idUsuario = 2, estado = estado, nroGuia = "G-000001", fechaGuia = fechaGuia, matricula = "STA1234",
        origenLat = -34.9, origenLon = -56.1, destinoLat = -30.9, destinoLon = -55.5, volumen = 18000,
        rubro = "Agropecuario", tipoCarga = "Granos"
    )

    @Test
    fun asignado_soloSePuedeIniciar() {
        val acciones = accionesPara(viaje("ASIGNADO"), hayOtroEnCurso = false, hoy = hoy)
        assertEquals(listOf(TipoEvento.INICIO), acciones.eventos)
        assertNull(acciones.bloqueo)
    }

    @Test
    fun asignado_conOtroEnCurso_quedaBloqueado() {
        val acciones = accionesPara(viaje("ASIGNADO"), hayOtroEnCurso = true, hoy = hoy)
        assertEquals("Tenés otro viaje en curso. Finalizalo para iniciar este.", acciones.bloqueo)
    }

    @Test
    fun asignado_antesDelDiaDeLaGuia_quedaBloqueado() {
        val acciones = accionesPara(viaje("ASIGNADO", fechaGuia = "2026-10-28"), hayOtroEnCurso = false, hoy = hoy)
        assertEquals("Se puede iniciar desde el 28/10.", acciones.bloqueo)
    }

    @Test
    fun asignado_conUnaFechaQueNoSeEntiende_noSeBloqueaYDecideElCentral() {
        assertNull(accionesPara(viaje("ASIGNADO", fechaGuia = ""), hayOtroEnCurso = false, hoy = hoy).bloqueo)
    }

    @Test
    fun enCurso_ofreceCargasIncidenteYFin() {
        val acciones = accionesPara(viaje("EN_CURSO"), hayOtroEnCurso = false, hoy = hoy)
        assertEquals(
            listOf(TipoEvento.CARGA_PARCIAL, TipoEvento.DESCARGA_PARCIAL, TipoEvento.INCIDENTE, TipoEvento.FIN),
            acciones.eventos
        )
    }

    @Test
    fun finalizado_noOfreceNada() {
        assertTrue(accionesPara(viaje("FINALIZADO"), hayOtroEnCurso = false, hoy = hoy).eventos.isEmpty())
    }

    @Test
    fun incidente_pideDescripcion() {
        assertEquals("Describí qué pasó.", errorDelEvento(TipoEvento.INCIDENTE, null, "   ", 18000))
        assertNull(errorDelEvento(TipoEvento.INCIDENTE, null, "Pinchadura", 18000))
    }

    @Test
    fun descarga_pideKilosEntreUnoYElVolumen() {
        assertEquals("Indicá los kilos.", errorDelEvento(TipoEvento.DESCARGA_PARCIAL, null, "", 18000))
        assertEquals("Los kilos tienen que ser más que 0.", errorDelEvento(TipoEvento.DESCARGA_PARCIAL, 0, "", 18000))
        assertEquals(
            "No puede superar el volumen de la guía (18.000 kg).",
            errorDelEvento(TipoEvento.DESCARGA_PARCIAL, 18001, "", 18000)
        )
        assertNull(errorDelEvento(TipoEvento.DESCARGA_PARCIAL, 18000, "", 18000))
    }

    @Test
    fun laDescripcionTieneUnMaximo() {
        val larga = "x".repeat(DESCRIPCION_MAXIMA + 1)
        assertEquals("La descripción puede tener hasta 500 caracteres.", errorDelEvento(TipoEvento.CARGA_PARCIAL, 10, larga, 18000))
    }
}
