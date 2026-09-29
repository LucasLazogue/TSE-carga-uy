package uy.tse.cargauy.ui.screens.inicio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

import uy.tse.cargauy.CargaUYApp

class InicioViewModel(application: Application) : AndroidViewModel(application) {

    private val eventoPendienteDao = (application as CargaUYApp).database.eventoPendienteDao()

    /** Se actualiza solo cada vez que cambia la tabla evento_pendiente. */
    val eventosPendientes: StateFlow<Int> = eventoPendienteDao.contar()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
