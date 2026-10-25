package uy.tse.cargauy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// todo filtra por usuario: los eventos de un chofer se ven solo con su sesion, y se envian con su sesion
// o, si ya no esta logueado, con su refresh token
@Dao
interface EventoPendienteDao {

    @Insert
    suspend fun insertar(evento: EventoPendiente)

    // los que faltan enviar, en el orden en que se generaron
    @Query("SELECT * FROM evento_pendiente WHERE idUsuario = :idUsuario AND resultado IS NULL ORDER BY timestampGeneracion")
    suspend fun pendientes(idUsuario: Long): List<EventoPendiente>

    // los choferes con eventos sin enviar, este o no logueado alguno de ellos
    @Query("SELECT DISTINCT idUsuario FROM evento_pendiente WHERE resultado IS NULL")
    suspend fun usuariosConPendientes(): List<Long>

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
