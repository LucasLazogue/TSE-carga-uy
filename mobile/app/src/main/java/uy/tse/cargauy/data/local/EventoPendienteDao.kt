package uy.tse.cargauy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// todo filtra por usuario: en un celular compartido cada chofer ve solo lo suyo
@Dao
interface EventoPendienteDao {

    @Insert
    suspend fun insertar(evento: EventoPendiente)

    // los que faltan enviar, en el orden en que se generaron
    @Query("SELECT * FROM evento_pendiente WHERE idUsuario = :idUsuario AND resultado IS NULL ORDER BY timestampGeneracion")
    suspend fun pendientes(idUsuario: Long): List<EventoPendiente>

    @Query("SELECT DISTINCT idUsuario FROM evento_pendiente WHERE resultado IS NULL")
    suspend fun usuariosConPendientes(): List<Long>

    @Query("SELECT COUNT(*) FROM evento_pendiente WHERE idUsuario = :idUsuario AND resultado IS NULL")
    fun contar(idUsuario: Long): Flow<Int>

    @Query("SELECT * FROM evento_pendiente WHERE idUsuario = :idUsuario AND idViaje = :idViaje ORDER BY timestampGeneracion DESC")
    fun observarDelViaje(idUsuario: Long, idViaje: Long): Flow<List<EventoPendiente>>

    @Query(
        "SELECT * FROM evento_pendiente WHERE idUsuario = :idUsuario AND resultado IN ('CONFLICTO', 'RECHAZADO') " +
            "AND idViaje NOT IN (SELECT idViaje FROM viaje_local WHERE idUsuario = :idUsuario) " +
            "ORDER BY timestampGeneracion DESC"
    )
    fun observarNoAplicadosSinViaje(idUsuario: Long): Flow<List<EventoPendiente>>

    @Query("UPDATE evento_pendiente SET resultado = :resultado, mensaje = :mensaje WHERE idEvento = :idEvento")
    suspend fun marcar(idEvento: String, resultado: String, mensaje: String?)

    @Query(
        "DELETE FROM evento_pendiente WHERE idUsuario = :idUsuario AND resultado IN ('APLICADO', 'DUPLICADO') " +
            "AND idViaje NOT IN (SELECT idViaje FROM viaje_local WHERE idUsuario = :idUsuario)"
    )
    suspend fun borrarAplicadosSinViaje(idUsuario: Long)

    @Query(
        "DELETE FROM evento_pendiente WHERE idUsuario = :idUsuario AND resultado IN ('CONFLICTO', 'RECHAZADO') " +
            "AND idViaje NOT IN (SELECT idViaje FROM viaje_local WHERE idUsuario = :idUsuario)"
    )
    suspend fun borrarNoAplicadosSinViaje(idUsuario: Long)
}
