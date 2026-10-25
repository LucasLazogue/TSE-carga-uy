package uy.tse.cargauy.data.auth

import java.io.IOException
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

import uy.tse.cargauy.CargaUYApp
import uy.tse.cargauy.data.local.RefreshToken
import uy.tse.cargauy.data.remote.ApiClient
import uy.tse.cargauy.data.remote.AuthApi
import uy.tse.cargauy.data.remote.TokenRespuesta

/**
 * Los refresh tokens de los choferes que ingresaron en este celular. El del logueado renueva su sesión
 * (ApiClient); el de otro chofer sirve para enviar sus eventos aunque ya no esté logueado (offline access).
 */
class RefreshTokens(private val app: CargaUYApp) {

    private val dao get() = app.database.refreshTokenDao()

    private suspend fun authApi(): AuthApi =
        ApiClient.sinSesion(app.preferencias.urlServidor.first()).create(AuthApi::class.java)

    suspend fun guardar(idUsuario: Long, token: String) = dao.guardar(RefreshToken(idUsuario, token))

    // un access token nuevo del chofer (RFC 6749 6). null si no tiene refresh token o si el central lo rechazo
    // (vencido, revocado o de otro celular): en ese caso se borra, porque no va a volver a servir.
    // sin red tira IOException; si el central falla, HttpException
    suspend fun renovar(idUsuario: Long): TokenRespuesta? {
        val refresh = dao.de(idUsuario) ?: return null
        return try {
            authApi().refrescar("refresh_token", refresh.token, app.preferencias.idDispositivo())
        } catch (e: HttpException) {
            if (e.code() >= 500) throw e
            dao.borrar(idUsuario)
            null
        }
    }

    // RFC 7009. se borra del celular aunque el central no conteste: si no se entero, vence solo por inactividad
    suspend fun revocar(idUsuario: Long) {
        val refresh = dao.de(idUsuario) ?: return
        try {
            authApi().revocar(refresh.token)
        } catch (e: IOException) {
            // sin red: queda activo en el central hasta que venza
        } catch (e: HttpException) {
            // el central fallo: igual que sin red
        }
        dao.borrar(idUsuario)
    }
}
