package uy.tse.cargauy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// todo filtra por usuario: en un celular compartido cada chofer ve y modifica solo su viaje
@Dao
interface ViajeLocalDao {

    // cada chofer tiene un solo viaje actual a la vez
    @Query("SELECT * FROM viaje_local WHERE idUsuario = :idUsuario LIMIT 1")
    fun observar(idUsuario: Long): Flow<ViajeLocal?>

    @Query("SELECT * FROM viaje_local WHERE idUsuario = :idUsuario LIMIT 1")
    suspend fun actual(idUsuario: Long): ViajeLocal?

    @Insert
    suspend fun insertar(viaje: ViajeLocal)

    @Query("DELETE FROM viaje_local WHERE idUsuario = :idUsuario")
    suspend fun borrar(idUsuario: Long)

    @Query("UPDATE viaje_local SET estado = :estado WHERE idUsuario = :idUsuario")
    suspend fun cambiarEstado(idUsuario: Long, estado: String)

    // borrar y guardar juntos: si el central le asigna otro viaje, no queda el anterior en la tabla
    @Transaction
    suspend fun reemplazar(viaje: ViajeLocal) {
        borrar(viaje.idUsuario)
        insertar(viaje)
    }
}
