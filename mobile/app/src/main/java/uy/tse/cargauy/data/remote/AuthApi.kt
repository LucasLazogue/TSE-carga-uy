package uy.tse.cargauy.data.remote

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    // canje del code por los tokens (PKCE): AuthRest.token en el backend
    @FormUrlEncoded
    @POST("auth/token")
    suspend fun token(
        @Header("DPoP") dpop: String,
        @Field("grant_type") tipo: String,
        @Field("code") code: String,
        @Field("code_verifier") verifier: String
    ): TokenRespuesta

    // RFC 6749 6
    @FormUrlEncoded
    @POST("auth/token")
    suspend fun refrescar(
        @Header("DPoP") dpop: String,
        @Field("grant_type") tipo: String,
        @Field("refresh_token") refreshToken: String
    ): TokenRespuesta

    // RFC 7009
    @FormUrlEncoded
    @POST("auth/revoke")
    suspend fun revocar(@Field("token") token: String): Response<Unit>

    // para preguntarle al central si la sesion guardada sigue sirviendo; el cuerpo no se usa.
    // Response y no el DTO: un 401 no tira excepcion (ApiClient renueva o cierra la sesion)
    @GET("usuarios/me")
    suspend fun usuarioActual(): Response<Unit>
}
