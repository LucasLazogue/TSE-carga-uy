package uy.tse.cargauy.data.remote

import java.io.IOException
import retrofit2.HttpException

fun mensajeDeError(e: Exception): String = when (e) {
    is HttpException -> e.response()?.errorBody()?.string()
        ?.let { cuerpo -> runCatching { ApiClient.json.decodeFromString<ErrorDto>(cuerpo).mensaje }.getOrNull() }
        ?.takeIf { it.isNotBlank() }
        ?: "El servidor respondió con un error (${e.code()})."
    is IOException -> "No se pudo conectar con el servidor. Revisá la conexión y la URL."
    else -> "Ocurrió un error inesperado."
}
