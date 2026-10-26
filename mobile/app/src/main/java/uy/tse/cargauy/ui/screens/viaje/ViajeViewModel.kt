package uy.tse.cargauy.ui.screens.viaje

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import uy.tse.cargauy.CargaUYApp
import uy.tse.cargauy.data.EstadoViaje
import uy.tse.cargauy.data.TipoEvento
import uy.tse.cargauy.data.ViajeRepository
import uy.tse.cargauy.data.local.EventoPendiente
import uy.tse.cargauy.data.local.ViajeLocal
import uy.tse.cargauy.data.remote.hayRed
import uy.tse.cargauy.ui.UiState

// navigation deja el id de la ruta en el SavedStateHandle
class ViajeViewModel(application: Application, estado: SavedStateHandle) : AndroidViewModel(application) {

    private val repositorio = ViajeRepository(application as CargaUYApp)
    private val idViaje: Long = checkNotNull(estado[ARGUMENTO])

    // null si el viaje salio de la lista
    val viaje: StateFlow<UiState<ViajeLocal?>> = repositorio.viaje(idViaje)
        .map<ViajeLocal?, UiState<ViajeLocal?>> { UiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val hayOtroEnCurso: StateFlow<Boolean> = repositorio.viajes
        .map { lista -> lista.any { it.idViaje != idViaje && it.estado == EstadoViaje.EN_CURSO.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val eventos: StateFlow<List<EventoPendiente>> = repositorio.eventosDe(idViaje)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val conRed: StateFlow<Boolean> = hayRed(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun registrar(tipo: TipoEvento, descripcion: String? = null, kilos: Int? = null) {
        viewModelScope.launch { repositorio.registrarEvento(idViaje, tipo, descripcion, kilos) }
    }

    companion object {
        const val ARGUMENTO = "idViaje"
    }
}
