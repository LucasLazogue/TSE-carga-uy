package uy.tse.cargauy.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoPendienteDao {

    @Query("SELECT COUNT(*) FROM evento_pendiente")
    fun contar(): Flow<Int>
}
