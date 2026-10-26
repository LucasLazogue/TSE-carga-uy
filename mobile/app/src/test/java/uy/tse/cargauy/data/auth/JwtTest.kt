package uy.tse.cargauy.data.auth

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JwtTest {

    // arma un token con el formato del central: cabecera.payload.firma, cada parte en Base64 URL sin relleno
    private fun token(payload: String): String {
        val codificar = { texto: String -> Base64.getUrlEncoder().withoutPadding().encodeToString(texto.toByteArray()) }
        return "${codificar("""{"alg":"HS256"}""")}.${codificar(payload)}.firma"
    }

    @Test
    fun leeElIdDelUsuarioDelClaimSub() {
        val jwt = token("""{"cedula":"55555555","roles":["CHOFER"],"sub":"2","typ":"sesion"}""")
        assertEquals(2L, idUsuarioDelToken(jwt))
    }

    @Test
    fun leeQuienEstaLogueado() {
        val jwt = token("""{"cedula":"55555555","correo":"carlos.suarez@example.com","sub":"2","typ":"sesion"}""")
        assertEquals(UsuarioDelToken(2, "55555555", "carlos.suarez@example.com"), usuarioDelToken(jwt))
    }

    @Test
    fun sinCorreoQuedaEnNull() {
        val usuario = usuarioDelToken(token("""{"cedula":"55555555","sub":"2"}"""))
        assertNull(usuario?.correo)
        assertEquals("55555555", usuario?.cedula)
    }

    @Test
    fun sinSubNoHayUsuario() {
        assertNull(usuarioDelToken(token("""{"cedula":"55555555"}""")))
        assertNull(usuarioDelToken("no-es-un-jwt"))
    }

    @Test
    fun tokenMalFormadoDevuelveNull() {
        assertNull(idUsuarioDelToken("no-es-un-jwt"))
        assertNull(idUsuarioDelToken(token("""{"cedula":"55555555"}"""))) // sin sub
    }
}
