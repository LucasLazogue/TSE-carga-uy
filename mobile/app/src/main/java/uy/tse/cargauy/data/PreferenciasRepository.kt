package uy.tse.cargauy.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

import uy.tse.cargauy.BuildConfig

private val Context.dataStore by preferencesDataStore(name = "preferencias")

class PreferenciasRepository( private val context: Context) {
    private val temaOscuroKey = booleanPreferencesKey("tema_oscuro")
    private val urlServidorKey = stringPreferencesKey("url_servidor")

    val temaOscuro: Flow<Boolean> = context.dataStore.data.map {prefs -> prefs[temaOscuroKey] ?: false}

    // debug: la que guardo el usuario en Configuracion o, si no guardo ninguna, la del build (local.properties).
    // release: siempre la del build, aunque haya una guardada de un debug instalado antes
    val urlServidor: Flow<String> =
        if (BuildConfig.DEBUG) context.dataStore.data.map {prefs -> prefs[urlServidorKey] ?: BuildConfig.URL_SERVIDOR}
        else flowOf(BuildConfig.URL_SERVIDOR)

    suspend fun guardarTemaOscuro(valor: Boolean) {
        context.dataStore.edit {
            prefs -> prefs[temaOscuroKey] = valor
        }
    }

    suspend fun guardarUrlServidor(url: String) {
        context.dataStore.edit {
            prefs -> prefs[urlServidorKey] = normalizarUrl(url)
        }
    }

    // borra la guardada para volver a la del build
    suspend fun restaurarUrlServidor() {
        context.dataStore.edit {
            prefs -> prefs.remove(urlServidorKey)
        }
    }

    companion object {
        // retrofit exige que la url base termine en /
        fun normalizarUrl(url: String): String {
            val limpia = url.trim()
            return if (limpia.endsWith("/")) limpia else "$limpia/"
        }
    }
}
