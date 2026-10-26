package uy.tse.cargauy.ui.navigation

import kotlinx.serialization.Serializable

// cada pantalla es una clase: sus parametros son campos con tipo, y navigation los guarda y los lee solo
object Rutas {
    @Serializable data object Login
    @Serializable data object Viajes
    @Serializable data class Viaje(val idViaje: Long)
    @Serializable data object Configuracion
}
