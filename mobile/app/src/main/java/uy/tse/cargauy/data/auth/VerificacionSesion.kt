package uy.tse.cargauy.data.auth

import java.io.IOException
import kotlinx.coroutines.flow.first

import uy.tse.cargauy.CargaUYApp
import uy.tse.cargauy.data.remote.ApiClient
import uy.tse.cargauy.data.remote.AuthApi

// ante un 401, ApiClient renueva la sesion o, si el central rechaza el refresh token, la cierra y aparece el login
suspend fun verificarSesionConCentral(app: CargaUYApp) {
    val urlServidor = app.preferencias.urlServidor.first()
    if (urlServidor.isBlank()) return
    try {
        ApiClient.crear(urlServidor, app.sesion, app.refreshTokens).create(AuthApi::class.java).usuarioActual()
    } catch (e: IOException) {
        // hay red pero el central no responde (servidor caido o red sin salida): se sigue trabajando
        // sin conexion. cerrar la sesion aca dejaria al chofer en un login que no puede completar
    }
}
