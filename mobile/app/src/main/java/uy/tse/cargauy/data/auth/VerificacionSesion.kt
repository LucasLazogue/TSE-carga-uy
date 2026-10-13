package uy.tse.cargauy.data.auth

import java.io.IOException
import kotlinx.coroutines.flow.first

import uy.tse.cargauy.data.PreferenciasRepository
import uy.tse.cargauy.data.remote.ApiClient
import uy.tse.cargauy.data.remote.AuthApi

// le pregunta al central si el token guardado sigue sirviendo. si vencio, responde 401 y ApiClient
// cierra la sesion: el login aparece solo, sin que el chofer tenga que hacer nada
suspend fun verificarSesionConCentral(preferencias: PreferenciasRepository, sesion: SesionRepository) {
    val urlServidor = preferencias.urlServidor.first()
    if (urlServidor.isBlank()) return
    try {
        ApiClient.crear(urlServidor, sesion).create(AuthApi::class.java).usuarioActual()
    } catch (e: IOException) {
        // hay red pero el central no responde (servidor caido o red sin salida): se sigue trabajando
        // sin conexion. cerrar la sesion aca dejaria al chofer en un login que no puede completar
    }
}
