package uy.tse.cargauy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "refresh_token")
data class RefreshToken(
    @PrimaryKey val idUsuario: Long,
    val token: String
)
