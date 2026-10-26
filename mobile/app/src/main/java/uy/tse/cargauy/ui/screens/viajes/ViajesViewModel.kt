package uy.tse.cargauy.ui.screens.viajes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException

import uy.tse.cargauy.CargaUYApp
import uy.tse.cargauy.data.ViajeRepository
import uy.tse.cargauy.data.local.EventoPendiente
import uy.tse.cargauy.data.local.ViajeLocal
import uy.tse.cargauy.data.remote.hayRed
import uy.tse.cargauy.data.remote.mensajeDeError
import uy.tse.cargauy.ui.UiState

class ViajesViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = ViajeRepository(application as CargaUYApp)

    // Loading solo mientras se lee Room la primera vez; despues lo guardado, aunque no haya red
    val viajes: StateFlow<UiState<List<ViajeLocal>>> = repositorio.viajes
        .map<List<ViajeLocal>, UiState<List<ViajeLocal>>> { UiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val pendientes: StateFlow<Int> = repositorio.pendientes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val noAplicados: StateFlow<List<EventoPendiente>> = repositorio.noAplicadosSinViaje
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val conRed: StateFlow<Boolean> = hayRed(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _actualizando = MutableStateFlow(false)
    val actualizando: StateFlow<Boolean> = _actualizando

    // por que fallo la ultima actualizacion; null si salio bien
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        // al abrir la pantalla (si hay red) y cada vez que vuelve la red
        viewModelScope.launch {
            hayRed(application).filter { it }.collect { actualizar() }
        }
    }

    fun actualizar() {
        viewModelScope.launch {
            _actualizando.value = true
            _error.value = try {
                repositorio.actualizar()
                null
            } catch (e: IOException) {
                "No se pudo conectar con el central: se muestran los últimos viajes guardados."
            } catch (e: HttpException) {
                mensajeDeError(e)
            } finally {
                _actualizando.value = false
            }
        }
    }

    fun descartarNoAplicados() {
        viewModelScope.launch { repositorio.descartarNoAplicados() }
    }
}
