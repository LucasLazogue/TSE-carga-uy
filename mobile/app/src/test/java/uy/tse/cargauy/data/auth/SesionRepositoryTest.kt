package uy.tse.cargauy.data.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SesionRepositoryTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    // un DataStore real sobre un archivo temporal y el reloj virtual del test, en vez del celular y la hora real
    private fun TestScope.repositorio() = SesionRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(carpeta.root, "sesion.preferences_pb") },
        ahora = { currentTime }
    )

    @Test
    fun elTokenVenceSegunExpiresIn() = runTest {
        val sesion = repositorio()
        sesion.guardarToken("abc", expiraEnSegundos = 60)
        assertFalse(sesion.vencida.first())

        advanceTimeBy(60_000)
        assertTrue(sesion.vencida.first())
    }

    // V1.a: con el token vencido el chofer sigue trabajando; quien lo rechaza es el central (401)
    @Test
    fun tokenVencido_seSigueUsandoHastaQueElCentralLoRechace() = runTest {
        val sesion = repositorio()
        sesion.guardarToken("abc", expiraEnSegundos = 60)

        advanceTimeBy(7 * 60 * 60 * 1000L)

        assertEquals("abc", sesion.token.first())
        assertTrue(sesion.haySesion.first())
        assertTrue(sesion.vencida.first())
    }

    @Test
    fun rechazoDelCentral_seRecuerdaHastaElProximoIngreso() = runTest {
        val sesion = repositorio()
        sesion.guardarToken("abc", expiraEnSegundos = 60)

        sesion.cerrarSesion(rechazadaPorCentral = true)
        assertFalse(sesion.haySesion.first())
        assertFalse(sesion.vencida.first())
        assertTrue(sesion.rechazadaPorCentral.first())

        sesion.guardarToken("nuevo", expiraEnSegundos = 60)
        assertFalse(sesion.rechazadaPorCentral.first())
    }

    @Test
    fun cierreManual_noSeMuestraComoVencida() = runTest {
        val sesion = repositorio()
        sesion.guardarToken("abc", expiraEnSegundos = 60)

        sesion.cerrarSesion()

        assertNull(sesion.token.first())
        assertFalse(sesion.rechazadaPorCentral.first())
    }

    @Test
    fun verifier_sirveUnaSolaVez() = runTest {
        val sesion = repositorio()
        sesion.guardarVerifier("secreto")

        assertEquals("secreto", sesion.tomarVerifier())
        assertNull(sesion.tomarVerifier())
    }
}
