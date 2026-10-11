package uy.tse.cargauy.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PkceTest {

    // ejemplo del apendice B de la RFC 7636; el backend usa el mismo calculo (UsuarioEJB.challenge)
    @Test
    fun challenge_coincideConElEjemploDeLaRfc() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            Pkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
        )
    }

    @Test
    fun nuevoVerifier_esUrlSeguroYSinRelleno() {
        val verifier = Pkce.nuevoVerifier()
        // 32 bytes en base64 sin relleno son 43 caracteres; la RFC pide entre 43 y 128
        assertEquals(43, verifier.length)
        assertTrue(verifier.all { it.isLetterOrDigit() || it == '-' || it == '_' })
    }

    @Test
    fun nuevoVerifier_esDistintoCadaVez() {
        assertNotEquals(Pkce.nuevoVerifier(), Pkce.nuevoVerifier())
    }
}
