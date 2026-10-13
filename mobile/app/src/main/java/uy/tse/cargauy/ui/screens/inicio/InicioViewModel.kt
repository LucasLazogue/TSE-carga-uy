package uy.tse.cargauy.ui.screens.inicio

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
import uy.tse.cargauy.data.TipoEvento
import uy.tse.cargauy.data.ViajeRepository
import uy.tse.cargauy.data.local.EventoPendiente
import uy.tse.cargauy.data.local.ViajeLocal
import uy.tse.cargauy.data.remote.hayRed
import uy.tse.cargauy.data.remote.mensajeDeError
import uy.tse.cargauy.ui.UiState

class InicioViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = ViajeRepository(application as CargaUYApp)

    // Loading solo mientras se lee Room la primera vez; despues el viaje guardado, o null si no tiene
    val viaje: StateFlow<UiState<ViajeLocal?>> = repositorio.viaje
        .map<ViajeLocal?, UiState<ViajeLocal?>> { UiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val pendientes: StateFlow<Int> = repositorio.pendientes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val enviados: StateFlow<List<EventoPendiente>> = repositorio.enviados
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val conRed: StateFlow<Boolean> = hayRed(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // por que fallo la ultima actualizacion del viaje; null si salio bien
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
            _error.value = try {
                repositorio.actualizar()
                null
            } catch (e: IOException) {
                "No se pudo conectar con el central: se muestra el último viaje guardado."
            } catch (e: HttpException) {
                mensajeDeError(e)
            }
        }
    }

    fun registrar(tipo: TipoEvento) {
        viewModelScope.launch { repositorio.registrarEvento(tipo) }
    }

    fun borrarEnviados() {
        viewModelScope.launch { repositorio.borrarEnviados() }
    }
}
