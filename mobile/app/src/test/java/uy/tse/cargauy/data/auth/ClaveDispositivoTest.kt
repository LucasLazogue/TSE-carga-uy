package uy.tse.cargauy.data.auth

import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// sin Keystore (no existe en la JVM): una clave EC comun firmando igual que firma android, en DER
class ClaveDispositivoTest {

    private val par = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    private val publica = par.public as ECPublicKey
    private val url = "http://192.168.1.50:8080/carga-uy/api/auth/token"

    private fun decodificar(parte: String) = Base64.getUrlDecoder().decode(parte)

    private fun prueba() = armarPrueba(jwk(publica), "POST", url, 1_792_000_000, "un-jti") { datos ->
        derARaw(Signature.getInstance("SHA256withECDSA").apply { initSign(par.private); update(datos) }.sign())
    }

    @Test
    fun laPruebaTraeLoQuePideElEstandar() {
        val (header, cuerpo) = prueba().split(".").take(2).map { Json.parseToJsonElement(String(decodificar(it))).jsonObject }

        assertEquals("dpop+jwt", header["typ"]!!.jsonPrimitive.content)
        assertEquals("ES256", header["alg"]!!.jsonPrimitive.content)
        assertEquals("POST", cuerpo["htm"]!!.jsonPrimitive.content)
        assertEquals(url, cuerpo["htu"]!!.jsonPrimitive.content)
        assertEquals(1_792_000_000L, cuerpo["iat"]!!.jsonPrimitive.long)
        assertEquals("un-jti", cuerpo["jti"]!!.jsonPrimitive.content)
    }

    // lo mismo que hace el central: arma la clave publica con la jwk del header y verifica la firma con ella
    @Test
    fun laFirmaVerificaConLaClaveDelHeader() {
        val partes = prueba().split(".")
        val jwk = Json.parseToJsonElement(String(decodificar(partes[0]))).jsonObject["jwk"]!!.jsonObject
        val x = BigInteger(1, decodificar(jwk["x"]!!.jsonPrimitive.content))
        val y = BigInteger(1, decodificar(jwk["y"]!!.jsonPrimitive.content))
        val clave = KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(ECPoint(x, y), publica.params))

        // P1363 = R y S crudos, el formato de ES256
        val verificador = Signature.getInstance("SHA256withECDSAinP1363Format").apply {
            initVerify(clave)
            update("${partes[0]}.${partes[1]}".toByteArray(Charsets.US_ASCII))
        }
        assertTrue(verificador.verify(decodificar(partes[2])))
    }

    @Test
    fun lasCoordenadasDelJwkOcupan32Bytes() {
        val jwk = jwk(publica)

        assertEquals(32, decodificar(jwk["x"]!!.jsonPrimitive.content).size)
        assertEquals(32, decodificar(jwk["y"]!!.jsonPrimitive.content).size)
    }

    @Test
    fun a32Bytes_sacaElCeroDeSignoYRellenaLosCortos() {
        val conSigno = byteArrayOf(0) + ByteArray(32) { 0x80.toByte() }
        val corto = ByteArray(31) { 1 }

        assertArrayEquals(ByteArray(32) { 0x80.toByte() }, a32Bytes(conSigno))
        assertArrayEquals(byteArrayOf(0) + corto, a32Bytes(corto))
    }
}
