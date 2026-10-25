package uy.tse.cargauy.data.auth

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.HttpException

import uy.tse.cargauy.CargaUYApp
import uy.tse.cargauy.data.local.RefreshToken
import uy.tse.cargauy.data.remote.ApiClient
import uy.tse.cargauy.data.remote.AuthApi
import uy.tse.cargauy.data.remote.TokenRespuesta

/** Los pedidos a /auth/token y el refresh token de cada chofer que ingresó en el celular. */
class RefreshTokens(private val app: CargaUYApp) {

    private val dao get() = app.database.refreshTokenDao()

    private suspend fun urlServidor(): String = app.preferencias.urlServidor.first()

    private suspend fun authApi(): AuthApi = ApiClient.sinSesion(urlServidor()).create(AuthApi::class.java)

    // la prueba firma la url (RFC 9449 4.2): tiene que ser la misma que arma Retrofit.
    // fuera del hilo principal: el Keystore bloquea, y la primera vez genera la clave
    private suspend fun pruebaDpop(): String {
        val url = "${urlServidor()}auth/token"
        return withContext(Dispatchers.Default) { app.claveDispositivo.prueba("POST", url) }
    }

    suspend fun iniciar(code: String, verifier: String): TokenRespuesta {
        val respuesta = authApi().token(pruebaDpop(), "authorization_code", code, verifier)
        val idUsuario = idUsuarioDelToken(respuesta.accessToken)
        if (idUsuario != null && respuesta.refreshToken != null) dao.guardar(RefreshToken(idUsuario, respuesta.refreshToken))
        return respuesta
    }

    // null si no tiene refresh token o el central lo rechazo; en ese caso se borra, porque no va a volver a servir
    suspend fun renovar(idUsuario: Long): TokenRespuesta? {
        val refresh = dao.de(idUsuario) ?: return null
        return try {
            authApi().refrescar(pruebaDpop(), "refresh_token", refresh.token)
        } catch (e: HttpException) {
            if (e.code() >= 500) throw e
            dao.borrar(idUsuario)
            null
        }
    }

    // se borra aunque el central no conteste: si no se entero, vence solo por inactividad
    suspend fun revocar(idUsuario: Long) {
        val refresh = dao.de(idUsuario) ?: return
        try {
            authApi().revocar(refresh.token)
        } catch (e: IOException) {
        } catch (e: HttpException) {
        }
        dao.borrar(idUsuario)
    }
}
