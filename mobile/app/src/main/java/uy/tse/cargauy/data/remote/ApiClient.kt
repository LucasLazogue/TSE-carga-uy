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

    // con la sesion del chofer logueado. el Retrofit se arma por pedido porque la url sale de Configuracion y
    // puede cambiar; las conexiones son las de `base`. si el access token vencio (401), lo renueva con el refresh token y
    // repite el pedido una vez; si el central rechaza el refresh token, cierra la sesion y aparece el login
    fun crear(urlServidor: String, sesion: SesionRepository, refreshTokens: RefreshTokens): Retrofit {
        val autenticacion = Interceptor { chain ->
            // corre en un hilo de OkHttp y no en el de la pantalla, por eso runBlocking no congela la UI
            val token = runBlocking { sesion.token.first() }
            chain.proceed(conToken(chain.request(), token))
        }
        // OkHttp lo llama solo cuando el central responde 401
        val renovacion = object : Authenticator {
            override fun authenticate(route: Route?, response: Response): Request? {
                // ya se repitio con un token renovado y el central lo volvio a rechazar: no insistir
                if (response.priorResponse != null) return null
                val nuevo = runBlocking { renovar(sesion, refreshTokens) } ?: return null
                return conToken(response.request, nuevo)
            }
        }
        return construir(urlServidor, autenticacion, renovacion)
    }

    // sin sesion: el login, la renovacion y la revocacion
    fun sinSesion(urlServidor: String): Retrofit = construir(urlServidor, null, null)

    // con un access token fijo: el worker, para enviar los eventos de un chofer que no es el logueado.
    // sin renovacion: un 401 no tiene que tocar la sesion del que esta logueado
    fun crearConToken(urlServidor: String, token: String): Retrofit =
        construir(urlServidor, Interceptor { chain -> chain.proceed(conToken(chain.request(), token)) }, null)

    private suspend fun renovar(sesion: SesionRepository, refreshTokens: RefreshTokens): String? {
        val idUsuario = sesion.idUsuario.first() ?: return null
        val nuevo = try {
            refreshTokens.renovar(idUsuario)
        } catch (e: IOException) {
            // sin red o el central no responde: no es motivo para cerrar la sesion
            return null
        } catch (e: HttpException) {
            return null
        }
        if (nuevo == null) {
            // el central ya no acepta el refresh token: la app vuelve sola al login
            sesion.cerrarSesion(rechazadaPorCentral = true)
            return null
        }
        sesion.guardarToken(nuevo.accessToken, nuevo.refreshExpiresIn ?: nuevo.expiresIn)
        return nuevo.accessToken
    }

    private fun conToken(pedido: Request, token: String?): Request =
        if (token == null) pedido else pedido.newBuilder().header("Authorization", "Bearer $token").build()

    // uno solo para toda la app: los clientes que salen de el con newBuilder() comparten las conexiones abiertas
    // y los hilos, asi cada pedido no vuelve a abrir la conexion ni a negociar TLS
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
