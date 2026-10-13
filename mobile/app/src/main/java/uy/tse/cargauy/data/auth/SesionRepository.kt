package uy.tse.cargauy.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// archivo aparte de las preferencias: cerrar sesion lo borra entero sin tocar otras preferencias del usuario como el tema o la URL en debug
private val Context.sesionDataStore by preferencesDataStore(name = "sesion")

class SesionRepository(
    private val dataStore: DataStore<Preferences>,
    // la hora actual en milisegundos; los tests pasan un reloj simulado para no esperar horas
    private val ahora: () -> Long = System::currentTimeMillis
) {
    // el que usa la app: el archivo "sesion" del celular y el reloj real
    constructor(context: Context) : this(context.sesionDataStore)

    private val tokenKey = stringPreferencesKey("token")
    private val expiraKey = longPreferencesKey("token_expira")
    private val verifierKey = stringPreferencesKey("pkce_verifier")
    private val rechazadaKey = booleanPreferencesKey("rechazada_por_central")

    // true si la ultima sesion la cerro el central (401) y no el usuario: el login explica por que aparecio
    val rechazadaPorCentral: Flow<Boolean> = dataStore.data.map { prefs -> prefs[rechazadaKey] ?: false }

    // el JWT guardado, aunque haya vencido: si vencio, lo decide el central (responde 401 y ApiClient
    // cierra la sesion). asi el chofer sigue trabajando sin conexion con la sesion vencida (V1.a)
    val token: Flow<String?> = dataStore.data.map { prefs -> prefs[tokenKey] }

    // lo que mira la navegacion: el chofer ingreso en este celular y no cerro sesion (ni el central la rechazo)
    val haySesion: Flow<Boolean> = token.map { it != null }

    // el usuario logueado, sacado del token: cada evento y viaje guardado queda a su nombre
    val idUsuario: Flow<Long?> = token.map { it?.let(::idUsuarioDelToken) }

    // true desde el momento en que el token vence, para avisarle al chofer que al volver la señal
    // va a tener que ingresar de nuevo
    val vencida: Flow<Boolean> = vencimiento(dataStore.data.map { prefs -> prefs[expiraKey] }, ahora)

    // se guarda cuando vence y no cuanto dura: expires_in cuenta desde que el servidor lo emitio
    suspend fun guardarToken(token: String, expiraEnSegundos: Long) {
        dataStore.edit { prefs ->
            prefs[tokenKey] = token
            prefs[expiraKey] = ahora() + expiraEnSegundos * 1000
            prefs.remove(rechazadaKey)
        }
    }

    // rechazadaPorCentral: la cierra ApiClient al recibir un 401, no el usuario desde Configuracion
    suspend fun cerrarSesion(rechazadaPorCentral: Boolean = false) {
        dataStore.edit { prefs ->
            prefs.clear()
            if (rechazadaPorCentral) prefs[rechazadaKey] = true
        }
    }

    // en disco y no en una variable: android puede cerrar la app mientras el usuario esta en el navegador
    suspend fun guardarVerifier(verifier: String) {
        dataStore.edit { prefs -> prefs[verifierKey] = verifier }
    }

    // lo lee y lo borra en la misma transaccion: cada verifier sirve para un solo login
    suspend fun tomarVerifier(): String? {
        var verifier: String? = null
        dataStore.edit { prefs ->
            verifier = prefs[verifierKey]
            prefs.remove(verifierKey)
        }
        return verifier
    }
}

// true desde el momento en que vence la sesion. espera hasta la hora exacta porque vencer depende del
// reloj, no de un cambio en el disco. va aparte del repositorio para probarlo con un reloj simulado
fun vencimiento(expiraciones: Flow<Long?>, ahora: () -> Long): Flow<Boolean> = channelFlow {
    expiraciones.distinctUntilChanged().collectLatest { expira ->
        if (expira == null) {
            send(false)
            return@collectLatest
        }
        val falta = expira - ahora()
        if (falta > 0) {
            send(false)
            delay(falta)
        }
        send(true)
    }
}.distinctUntilChanged()
