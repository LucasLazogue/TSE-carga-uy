package uy.tse.cargauy.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenRespuesta(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Long,
    // solo en el login
    @SerialName("refresh_token") val refreshToken: String? = null,
    // extension: cuando deja de servir el refresh token
    @SerialName("refresh_expires_in") val refreshExpiresIn: Long? = null
)

@Serializable
data class ViajeDto(
    val id: Long,
    val estado: String,
    val nroGuia: String? = null,
    val matricula: String? = null,
    val fechaInicio: String? = null,
    val fechaFin: String? = null
)

@Serializable
data class GuiaDto(
    val id: Long,
    val nroGuia: String? = null,
    val fecha: String? = null,
    val origenLat: Double? = null,
    val origenLon: Double? = null,
    val destinoLat: Double? = null,
    val destinoLon: Double? = null,
    val volumen: Int = 0,
    val nombreRubro: String? = null,
    val nombreTipoCarga: String? = null
)

@Serializable
data class ViajeMovilDto(val viaje: ViajeDto, val guia: GuiaDto)

@Serializable
data class EventoSyncDto(
    val idEvento: String,
    val idViaje: Long,
    val tipo: String,
    val timestampGeneracion: String // ISO-8601 en UTC, por ejemplo 2026-10-15T11:00:00Z
)

@Serializable
data class ResultadoEventoDto(val idEvento: String, val resultado: String, val mensaje: String? = null)

@Serializable
data class SyncRespuestaDto(
    val resultados: List<ResultadoEventoDto> = emptyList(),
    val viaje: ViajeMovilDto? = null
)

// lo que responde el backend cuando algo falla: {"codigo": 4002, "mensaje": "..."}
@Serializable
data class ErrorDto(val codigo: Int = 0, val mensaje: String = "")
