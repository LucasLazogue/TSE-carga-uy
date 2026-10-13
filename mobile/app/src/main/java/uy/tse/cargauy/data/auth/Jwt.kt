package uy.tse.cargauy.data.auth

import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// el id del usuario que viaja en el token (claim "sub"), para saber de quien es cada evento guardado.
fun idUsuarioDelToken(token: String): Long? = runCatching {
    val payload = token.split(".")[1]
    val json = String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)
    Json.parseToJsonElement(json).jsonObject["sub"]?.jsonPrimitive?.content?.toLong()
}.getOrNull()
