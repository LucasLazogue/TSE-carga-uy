package uy.tse.cargauy.data.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.UUID
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** La clave DPoP (RFC 9449), una para todo el celular. La privada nunca sale del Keystore. */
class ClaveDispositivo {

    fun prueba(metodo: String, url: String): String {
        val par = par()
        return armarPrueba(jwk(par.public as ECPublicKey), metodo, url, System.currentTimeMillis() / 1000, UUID.randomUUID().toString()) { datos ->
            val firma = Signature.getInstance("SHA256withECDSA").apply {
                initSign(par.private)
                update(datos)
            }.sign()
            derARaw(firma)
        }
    }

    // synchronized: el login y el worker pueden pedirla a la vez y tienen que quedar con la misma
    @Synchronized
    private fun par(): KeyPair {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val privada = keyStore.getKey(ALIAS, null) as PrivateKey?
        if (privada != null) return KeyPair(keyStore.getCertificate(ALIAS).publicKey, privada)

        val generador = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE)
        generador.initialize(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1")) // P-256, el de ES256
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
        )
        return generador.generateKeyPair()
    }

    companion object {
        private const val KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "dpop"
    }
}

// lo que sigue no usa el Keystore: aparte para probarlo en la JVM

private fun b64(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

// RFC 7518 6.2.1
fun jwk(clave: ECPublicKey): JsonObject = buildJsonObject {
    put("kty", "EC")
    put("crv", "P-256")
    put("x", b64(a32Bytes(clave.w.affineX.toByteArray())))
    put("y", b64(a32Bytes(clave.w.affineY.toByteArray())))
}

// RFC 9449 4.2
fun armarPrueba(jwk: JsonObject, metodo: String, url: String, iat: Long, jti: String, firmar: (ByteArray) -> ByteArray): String {
    val header = buildJsonObject {
        put("typ", "dpop+jwt")
        put("alg", "ES256")
        put("jwk", jwk)
    }
    val cuerpo = buildJsonObject {
        put("jti", jti)
        put("htm", metodo)
        put("htu", url)
        put("iat", iat)
    }
    val datos = "${b64(header.toString().toByteArray(Charsets.UTF_8))}.${b64(cuerpo.toString().toByteArray(Charsets.UTF_8))}"
    return "$datos.${b64(firmar(datos.toByteArray(Charsets.US_ASCII)))}"
}

// android firma en DER (0x30 largo 0x02 largoR R 0x02 largoS S) y ES256 pide R y S crudos (RFC 7518 3.4).
// con P-256 cada largo entra en un byte
fun derARaw(der: ByteArray): ByteArray {
    require(der.size > 8 && der[0] == 0x30.toByte() && der[2] == 0x02.toByte()) { "la firma no es DER" }
    val largoR = der[3].toInt()
    val inicioS = 4 + largoR
    require(der[inicioS] == 0x02.toByte()) { "la firma no es DER" }
    val largoS = der[inicioS + 1].toInt()
    val r = der.copyOfRange(4, inicioS)
    val s = der.copyOfRange(inicioS + 2, inicioS + 2 + largoS)
    return a32Bytes(r) + a32Bytes(s)
}

// DER y BigInteger agregan un 0 de signo, o tienen menos bytes si el numero empieza con ceros
fun a32Bytes(bytes: ByteArray): ByteArray = when {
    bytes.size == 32 -> bytes
    bytes.size > 32 -> bytes.copyOfRange(bytes.size - 32, bytes.size)
    else -> ByteArray(32 - bytes.size) + bytes
}
