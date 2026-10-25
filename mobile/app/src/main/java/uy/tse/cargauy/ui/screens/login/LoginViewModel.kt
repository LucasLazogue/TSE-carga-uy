package uy.tse.cargauy.ui.screens.login

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException

import uy.tse.cargauy.CargaUYApp
import uy.tse.cargauy.data.auth.Pkce
import uy.tse.cargauy.data.remote.mensajeDeError
import uy.tse.cargauy.sync.Sincronizacion
import uy.tse.cargauy.ui.UiState

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CargaUYApp

    // null: esperando que toque Ingresar; Loading: canjeando el code; Error: el mensaje a mostrar.
    // no hay Success: al guardarse el token, la navegacion lleva sola a la pantalla principal
    private val _estado = MutableStateFlow<UiState<Unit>?>(null)
    val estado: StateFlow<UiState<Unit>?> = _estado

    // true si se llego al login porque el central rechazo la sesion (vencio), para explicarselo al chofer
    val sesionRechazada: StateFlow<Boolean> = app.sesion.rechazadaPorCentral
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // arma la url del login y guarda el verifier para el canje; null si falta la url del servidor
    // cedulaPrueba: solo para el login simulado en debug; vacia entra como el chofer 55555555
    suspend fun urlLogin(cedulaPrueba: String = ""): String? {
        val urlServidor = app.preferencias.urlServidor.first()
        if (urlServidor.isBlank()) {
            _estado.value = UiState.Error("Falta la URL del servidor: configurala con el engranaje de arriba.")
            return null
        }
        val verifier = Pkce.nuevoVerifier()
        app.sesion.guardarVerifier(verifier)
        _estado.value = null
        val url = "${urlServidor}auth/login?cliente=mobile&code_challenge=${Pkce.challenge(verifier)}"
        // TODO mock: sacar con gub.uy real (el central ignora la cedula si GUBUY_MOCK=false)
        return if (cedulaPrueba.isBlank()) url else "$url&cedula=${cedulaPrueba.trim()}"
    }

    // la vuelta del login: cargauy://ingreso?code=... si salio bien, cargauy://ingreso?error=4002 si no
    fun canjear(uri: Uri) {
        viewModelScope.launch {
            val code = uri.getQueryParameter("code")
            if (code == null) {
                _estado.value = UiState.Error(mensajeLogin(uri.getQueryParameter("error")))
                return@launch
            }
            // sin verifier no se puede canjear: el code es de otro intento o ya se uso
            val verifier = app.sesion.tomarVerifier()
            if (verifier == null) {
                _estado.value = UiState.Error(mensajeLogin(SOLICITUD_INVALIDA))
                return@launch
            }

            _estado.value = UiState.Loading
            try {
                val respuesta = app.refreshTokens.iniciar(code, verifier)
                // la sesion vence con el refresh token
                app.sesion.guardarToken(respuesta.accessToken, respuesta.refreshExpiresIn ?: respuesta.expiresIn)
                // si quedaron eventos sin enviar (por ejemplo, la sesion vencio sin señal), se envian ahora
                Sincronizacion.programar(app)
                _estado.value = null
            } catch (e: HttpException) {
                _estado.value = UiState.Error(mensajeDeError(e))
            } catch (e: IOException) {
                _estado.value = UiState.Error(mensajeDeError(e))
            }
        }
    }

    companion object {
        private const val SOLICITUD_INVALIDA = "4002"

        // el central vuelve con ?error=<numero de CodigoError> cuando el ingreso falla
        fun mensajeLogin(codigo: String?): String = when (codigo) {
            "4002" -> "El ingreso no es válido o expiró. Volvé a intentarlo."
            "4003" -> "No se pudo validar tu identidad con gub.uy."
            "4004" -> "gub.uy no está disponible. Probá de nuevo más tarde."
            else -> "No se pudo ingresar (código ${codigo ?: "desconocido"})."
        }
    }
}
