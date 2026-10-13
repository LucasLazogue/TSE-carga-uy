package uy.tse.cargauy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Evento registrado por el chofer, con lo que respondió el componente central al enviarlo. */
@Entity(tableName = "evento_pendiente")
data class EventoPendiente(
    // Generado en el dispositivo (UUID): reenviar el mismo evento no genera duplicados en el central.
    @PrimaryKey val idEvento: String,
    // el chofer que lo registro: se envia solo con su sesion, aunque despues ingrese otro en el celular
    val idUsuario: Long,
    val idViaje: Long,
    val tipo: String,
    val timestampGeneracion: Long,
    // null mientras no se envió; despues APLICADO, DUPLICADO, CONFLICTO o RECHAZADO
    val resultado: String? = null,
    // el motivo que manda el central cuando no lo aplica
    val mensaje: String? = null
)
