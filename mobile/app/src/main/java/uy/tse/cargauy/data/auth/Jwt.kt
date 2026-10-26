package uy.tse.cargauy.data.auth

import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class UsuarioDelToken(val id: Long, val cedula: String?, val correo: String?)

// la app no verifica la firma: no tiene el secreto
private fun payload(token: String): JsonObject {
    val parte = token.split(".")[1]
    return Json.parseToJsonElement(String(Base64.getUrlDecoder().decode(parte), Charsets.UTF_8)).jsonObject
}

// el id del usuario que viaja en el token (claim "sub"), para saber de quien es cada evento guardado.
fun idUsuarioDelToken(token: String): Long? = runCatching {
    payload(token)["sub"]?.jsonPrimitive?.content?.toLong()
}.getOrNull()

fun usuarioDelToken(token: String): UsuarioDelToken? = runCatching {
    val claims = payload(token)
    val id = claims["sub"]?.jsonPrimitive?.content?.toLong() ?: return@runCatching null
    UsuarioDelToken(
        id = id,
        cedula = claims["cedula"]?.jsonPrimitive?.content,
        correo = claims["correo"]?.jsonPrimitive?.content
    )
}.getOrNull()
