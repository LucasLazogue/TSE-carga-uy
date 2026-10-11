package uy.tse.cargauy.data.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// runTest usa tiempo virtual: los delay no esperan de verdad y currentTime es la hora simulada,
// asi se prueban 6 horas de sesion en milisegundos
@OptIn(ExperimentalCoroutinesApi::class)
class VencimientoTest {

    private val seisHoras = 6 * 60 * 60 * 1000L

    @Test
    fun sinSesion_noEstaVencida() = runTest {
        assertFalse(vencimiento(flowOf(null)) { currentTime }.first())
    }

    @Test
    fun yaVencida_loDiceEnseguida() = runTest {
        advanceTimeBy(seisHoras)
        assertTrue(vencimiento(flowOf(seisHoras - 1)) { currentTime }.first())
    }

    @Test
    fun venceALaHoraExactaSinQueCambieNada() = runTest {
        val valores = mutableListOf<Boolean>()
        backgroundScope.launch { vencimiento(flowOf(seisHoras)) { currentTime }.toList(valores) }

        runCurrent()
        assertEquals(listOf(false), valores)

        advanceTimeBy(seisHoras - 1) // un milisegundo antes
        runCurrent()
        assertEquals(listOf(false), valores)

        advanceTimeBy(1) // la hora exacta
        runCurrent()
        assertEquals(listOf(false, true), valores)
    }

    @Test
    fun alIngresarDeNuevoDejaDeEstarVencida() = runTest {
        val expiraciones = MutableStateFlow<Long?>(1_000L)
        val valores = mutableListOf<Boolean>()
        backgroundScope.launch { vencimiento(expiraciones) { currentTime }.toList(valores) }

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(listOf(false, true), valores)

        expiraciones.value = currentTime + seisHoras // nuevo token
        runCurrent()
        assertEquals(listOf(false, true, false), valores)
    }

    @Test
    fun alCerrarSesionDejaDeEstarVencida() = runTest {
        val expiraciones = MutableStateFlow<Long?>(0L)
        val valores = mutableListOf<Boolean>()
        backgroundScope.launch { vencimiento(expiraciones) { currentTime }.toList(valores) }
        runCurrent()

        expiraciones.value = null // cerrarSesion borra el vencimiento
        runCurrent()
        assertEquals(listOf(true, false), valores)
    }
}
