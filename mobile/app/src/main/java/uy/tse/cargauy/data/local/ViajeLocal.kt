package uy.tse.cargauy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** El viaje actual de un chofer y los datos de su Guía, para trabajar sin conexión (V1.a). */
@Entity(tableName = "viaje_local")
data class ViajeLocal(
    @PrimaryKey val idViaje: Long,
    val idUsuario: Long,
    val estado: String,
    val nroGuia: String,
    val fechaGuia: String,
    val matricula: String,
    val origenLat: Double,
    val origenLon: Double,
    val destinoLat: Double,
    val destinoLon: Double,
    val volumen: Int,
    val rubro: String,
    val tipoCarga: String
)
