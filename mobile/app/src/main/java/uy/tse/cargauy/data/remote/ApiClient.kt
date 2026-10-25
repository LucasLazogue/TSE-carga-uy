package uy.tse.cargauy.data.remote

import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

import uy.tse.cargauy.BuildConfig
import uy.tse.cargauy.data.auth.RefreshTokens
import uy.tse.cargauy.data.auth.SesionRepository

object ApiClient {

    // ignoreUnknownKeys: si el backend agrega un campo, la app no se rompe
    // coerceInputValues: un null donde la app espera un valor usa el valor por defecto del DTO
    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // si el access token vencio (401), lo renueva con el refresh token y repite el pedido una vez
    fun crear(urlServidor: String, sesion: SesionRepository, refreshTokens: RefreshTokens): Retrofit {
        val autenticacion = Interceptor { chain ->
            // corre en un hilo de OkHttp y no en el de la pantalla, por eso runBlocking no congela la UI
            val token = runBlocking { sesion.token.first() }
            chain.proceed(conToken(chain.request(), token))
        }
        val renovacion = object : Authenticator {
            override fun authenticate(route: Route?, response: Response): Request? {
                // ya se repitio con un token renovado: no insistir
                if (response.priorResponse != null) return null
                val nuevo = runBlocking { renovar(sesion, refreshTokens) } ?: return null
                return conToken(response.request, nuevo)
            }
        }
        return construir(urlServidor, autenticacion, renovacion)
    }

    fun sinSesion(urlServidor: String): Retrofit = construir(urlServidor, null, null)

    // sin renovacion: un 401 no tiene que tocar la sesion del logueado
    fun crearConToken(urlServidor: String, token: String): Retrofit =
        construir(urlServidor, Interceptor { chain -> chain.proceed(conToken(chain.request(), token)) }, null)

    private suspend fun renovar(sesion: SesionRepository, refreshTokens: RefreshTokens): String? {
        val idUsuario = sesion.idUsuario.first() ?: return null
        val nuevo = try {
            refreshTokens.renovar(idUsuario)
        } catch (e: IOException) {
            // sin red no se cierra la sesion
            return null
        } catch (e: HttpException) {
            return null
        }
        if (nuevo == null) {
            sesion.cerrarSesion(rechazadaPorCentral = true)
            return null
        }
        sesion.guardarToken(nuevo.accessToken, nuevo.refreshExpiresIn ?: nuevo.expiresIn)
        return nuevo.accessToken
    }

    private fun conToken(pedido: Request, token: String?): Request =
        if (token == null) pedido else pedido.newBuilder().header("Authorization", "Bearer $token").build()

    // los clientes que salen de el con newBuilder() comparten las conexiones
    private val base = OkHttpClient()

    // en debug se ve cada pedido y respuesta en Logcat (filtro OkHttp); en release no, porque incluye el token
    private val log = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private fun construir(urlServidor: String, autenticacion: Interceptor?, renovacion: Authenticator?): Retrofit {
        val cliente = base.newBuilder()
        autenticacion?.let { cliente.addInterceptor(it) }
        renovacion?.let { cliente.authenticator(it) }
        cliente.addInterceptor(log)

        return Retrofit.Builder()
            .baseUrl(urlServidor)
            .client(cliente.build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
