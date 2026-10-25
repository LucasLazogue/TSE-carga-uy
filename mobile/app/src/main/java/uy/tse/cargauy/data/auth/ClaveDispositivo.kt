package uy.tse.cargauy.data.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Date
import java.util.UUID

/** La clave DPoP (RFC 9449), una para todo el celular. La privada nunca sale del Keystore. */
class ClaveDispositivo {

    fun prueba(metodo: String, url: String): String {
        val par = par()
        return armarPrueba(par.public as ECPublicKey, par.private, metodo, url)
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
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
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

// aparte de la clase para probarla en la JVM, sin Keystore
fun armarPrueba(publica: ECPublicKey, privada: PrivateKey, metodo: String, url: String): String {
    val header = JWSHeader.Builder(JWSAlgorithm.ES256)
        .type(JOSEObjectType("dpop+jwt"))
        .jwk(ECKey.Builder(Curve.P_256, publica).build())
        .build()
    val cuerpo = JWTClaimsSet.Builder()
        .jwtID(UUID.randomUUID().toString())
        .claim("htm", metodo)
        .claim("htu", url)
        .issueTime(Date())
        .build()
    return SignedJWT(header, cuerpo).apply { sign(ECDSASigner(privada, Curve.P_256)) }.serialize()
}
