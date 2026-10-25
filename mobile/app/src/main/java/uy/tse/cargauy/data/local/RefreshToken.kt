package uy.tse.cargauy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * El refresh token de un chofer que ingresó en este celular (RFC 6749 1.5). Con él se piden access tokens
 * nuevos sin pasar por gub.uy, aunque ese chofer ya no sea el logueado: así se envían sus eventos.
 */
@Entity(tableName = "refresh_token")
data class RefreshToken(
    // uno por chofer: un login nuevo pisa al anterior, igual que en el central
    @PrimaryKey val idUsuario: Long,
    val token: String
)
