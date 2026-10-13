package uy.tse.cargauy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// todo filtra por usuario: los eventos de un chofer se ven y se envian solo con su sesion
@Dao
interface EventoPendienteDao {

    @Insert
    suspend fun insertar(evento: EventoPendiente)

    // los que faltan enviar, en el orden en que se generaron
    @Query("SELECT * FROM evento_pendiente WHERE idUsuario = :idUsuario AND resultado IS NULL ORDER BY timestampGeneracion")
    suspend fun pendientes(idUsuario: Long): List<EventoPendiente>

    @Query("SELECT COUNT(*) FROM evento_pendiente WHERE idUsuario = :idUsuario AND resultado IS NULL")
    fun contar(idUsuario: Long): Flow<Int>

    // los ya enviados con su resultado, el mas reciente primero
    @Query("SELECT * FROM evento_pendiente WHERE idUsuario = :idUsuario AND resultado IS NOT NULL ORDER BY timestampGeneracion DESC")
    fun observarEnviados(idUsuario: Long): Flow<List<EventoPendiente>>

    @Query("UPDATE evento_pendiente SET resultado = :resultado, mensaje = :mensaje WHERE idEvento = :idEvento")
    suspend fun marcar(idEvento: String, resultado: String, mensaje: String?)

    @Query("DELETE FROM evento_pendiente WHERE idUsuario = :idUsuario AND resultado IS NOT NULL")
    suspend fun borrarEnviados(idUsuario: Long)
}
