package uy.tse.cargauy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// todo filtra por usuario: en un celular compartido cada chofer ve y modifica solo sus viajes
@Dao
interface ViajeLocalDao {

    // fechaGuia es ISO: ordenar el texto ordena la fecha
    @Query(
        "SELECT * FROM viaje_local WHERE idUsuario = :idUsuario " +
            "ORDER BY CASE estado WHEN 'EN_CURSO' THEN 0 ELSE 1 END, fechaGuia, idViaje"
    )
    fun observarTodos(idUsuario: Long): Flow<List<ViajeLocal>>

    @Query("SELECT * FROM viaje_local WHERE idUsuario = :idUsuario AND idViaje = :idViaje")
    fun observar(idUsuario: Long, idViaje: Long): Flow<ViajeLocal?>

    @Query("SELECT * FROM viaje_local WHERE idUsuario = :idUsuario AND idViaje = :idViaje")
    suspend fun buscar(idUsuario: Long, idViaje: Long): ViajeLocal?

    @Insert
    suspend fun insertar(viajes: List<ViajeLocal>)

    @Query("DELETE FROM viaje_local WHERE idUsuario = :idUsuario")
    suspend fun borrar(idUsuario: Long)

    @Query("UPDATE viaje_local SET estado = :estado WHERE idUsuario = :idUsuario AND idViaje = :idViaje")
    suspend fun cambiarEstado(idUsuario: Long, idViaje: Long, estado: String)

    // el central manda la lista completa: un viaje que no viene no tiene que quedar
    @Transaction
    suspend fun reemplazarTodos(idUsuario: Long, viajes: List<ViajeLocal>) {
        borrar(idUsuario)
        insertar(viajes)
    }
}
