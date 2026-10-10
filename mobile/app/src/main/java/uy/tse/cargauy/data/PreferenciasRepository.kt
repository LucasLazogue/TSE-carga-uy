package uy.tse.cargauy.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "preferencias")

class PreferenciasRepository( private val context: Context) {
    private val temaOscuroKey = booleanPreferencesKey("tema_oscuro")
    private val urlServidor = stringPreferencesKey("")

    val temaOscuro: Flow<Boolean> = context.dataStore.data.map {prefs -> prefs[temaOscuroKey] ?: false}

    suspend fun guardarTemaOscuro(valor: Boolean) {
        context.dataStore.edit {
            prefs -> prefs[temaOscuroKey] = valor
        }
    }
}