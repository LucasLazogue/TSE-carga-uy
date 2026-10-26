package uy.tse.cargauy.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface MovilApi {
    @GET("movil/viajes")
    suspend fun viajes(): List<ViajeMovilDto>

    @POST("movil/sync")
    suspend fun sync(@Body eventos: List<EventoSyncDto>): SyncRespuestaDto
}
