package uy.tse.cargauy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RefreshTokenDao {

    @Query("SELECT * FROM refresh_token WHERE idUsuario = :idUsuario")
    suspend fun de(idUsuario: Long): RefreshToken?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(refreshToken: RefreshToken)

    @Query("DELETE FROM refresh_token WHERE idUsuario = :idUsuario")
    suspend fun borrar(idUsuario: Long)
}
