package uy.tse.cargauy.data.remote

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

import uy.tse.cargauy.BuildConfig
import uy.tse.cargauy.data.auth.SesionRepository

object ApiClient {

    // ignoreUnknownKeys: si el backend agrega un campo, la app no se rompe
    // coerceInputValues: un null donde la app espera un valor usa el valor por defecto del DTO
    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // se arma por pedido y no una sola vez porque la url sale de Configuracion y puede cambiar
    fun crear(urlServidor: String, sesion: SesionRepository): Retrofit {
        val autenticacion = Interceptor { chain ->
            // corre en un hilo de OkHttp y no en el de la pantalla, por eso runBlocking no congela la UI
            val token = runBlocking { sesion.token.first() }
            val pedido = if (token == null) chain.request()
            else chain.request().newBuilder().header("Authorization", "Bearer $token").build()

            val respuesta = chain.proceed(pedido)
            // 401 con token: vencio o el servidor ya no lo acepta. al borrarlo, la app vuelve sola al login
            if (respuesta.code == 401 && token != null) runBlocking { sesion.cerrarSesion(rechazadaPorCentral = true) }
            respuesta
        }

        // en debug se ve cada pedido y respuesta en Logcat (filtro OkHttp); en release no, porque incluye el token
        val log = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val cliente = OkHttpClient.Builder()
            .addInterceptor(autenticacion)
            .addInterceptor(log)
            .build()

        return Retrofit.Builder()
            .baseUrl(urlServidor)
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
