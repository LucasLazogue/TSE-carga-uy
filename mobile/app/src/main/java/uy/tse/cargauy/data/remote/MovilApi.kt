package uy.tse.cargauy.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface MovilApi {
    @GET("movil/viaje")
    suspend fun viaje(): Response<ViajeMovilDto>

    @POST("movil/sync")
    suspend fun sync(@Body eventos: List<EventoSyncDto>): SyncRespuestaDto
}
