package uy.tse.cargauy.data.remote

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    // canje del code por el access token y el refresh token (PKCE): AuthRest.token en el backend.
    // dispositivo: el refresh token queda atado a este celular
    @FormUrlEncoded
    @POST("auth/token")
    suspend fun token(
        @Field("grant_type") tipo: String,
        @Field("code") code: String,
        @Field("code_verifier") verifier: String,
        @Field("dispositivo") dispositivo: String
    ): TokenRespuesta

    // un access token nuevo con el refresh token, sin pasar por gub.uy (RFC 6749 6)
    @FormUrlEncoded
    @POST("auth/token")
    suspend fun refrescar(
        @Field("grant_type") tipo: String,
        @Field("refresh_token") refreshToken: String,
        @Field("dispositivo") dispositivo: String
    ): TokenRespuesta

    // RFC 7009: el central responde 200 aunque el token no exista; el cuerpo no se usa
    @FormUrlEncoded
    @POST("auth/revoke")
    suspend fun revocar(@Field("token") token: String): Response<Unit>

    // para preguntarle al central si la sesion guardada sigue sirviendo; el cuerpo no se usa.
    // Response y no el DTO: un 401 no tira excepcion (ApiClient renueva o cierra la sesion)
    @GET("usuarios/me")
    suspend fun usuarioActual(): Response<Unit>
}
