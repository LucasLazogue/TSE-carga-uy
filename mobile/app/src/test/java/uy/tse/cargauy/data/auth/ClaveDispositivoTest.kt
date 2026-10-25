package uy.tse.cargauy.data.auth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jwt.SignedJWT
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// sin Keystore (no existe en la JVM): una clave EC comun en su lugar
class ClaveDispositivoTest {

    private val par = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    private val url = "http://192.168.1.50:8080/carga-uy/api/auth/token"

    private fun prueba() = SignedJWT.parse(armarPrueba(par.public as ECPublicKey, par.private, "POST", url))

    @Test
    fun laPruebaTraeLoQuePideElEstandar() {
        val prueba = prueba()

        assertEquals("dpop+jwt", prueba.header.type.type)
        assertEquals(JWSAlgorithm.ES256, prueba.header.algorithm)
        assertEquals("POST", prueba.jwtClaimsSet.getStringClaim("htm"))
        assertEquals(url, prueba.jwtClaimsSet.getStringClaim("htu"))
        assertNotNull(prueba.jwtClaimsSet.jwtid)
        assertNotNull(prueba.jwtClaimsSet.issueTime)
    }

    // lo mismo que hace el central: verifica la firma con la clave publica que trae el header
    @Test
    fun laFirmaVerificaConLaClaveDelHeader() {
        val prueba = prueba()
        val jwk = prueba.header.jwk.toECKey()

        assertNull(jwk.d)
        assertTrue(prueba.verify(ECDSAVerifier(jwk)))
    }
}
