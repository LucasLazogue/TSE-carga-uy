package uy.tse.cargauy.data.remote

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    // canje del code por el token (PKCE): AuthRest.token en el backend
    @FormUrlEncoded
    @POST("auth/token")
    suspend fun token(
        @Field("grant_type") tipo: String,
        @Field("code") code: String,
        @Field("code_verifier") verifier: String
    ): TokenRespuesta

    // para preguntarle al central si el token guardado sigue sirviendo; el cuerpo no se usa.
    // Response y no el DTO: un 401 no tira excepcion (ApiClient ya cierra la sesion)
    @GET("usuarios/me")
    suspend fun usuarioActual(): Response<Unit>
}
