package uy.tse.cargauy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Evento registrado por el chofer que todavía no fue confirmado por el componente central. */
@Entity(tableName = "evento_pendiente")
data class EventoPendiente(
    // Generado en el dispositivo (UUID): reenviar el mismo evento no genera duplicados en el central.
    @PrimaryKey val idEvento: String,
    val idViaje: Long,
    val tipo: String,
    val timestampGeneracion: Long
)
