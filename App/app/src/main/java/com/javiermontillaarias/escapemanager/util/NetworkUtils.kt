package com.javiermontillaarias.escapemanager.util

import com.google.gson.JsonSyntaxException
import com.javiermontillaarias.escapemanager.BuildConfig
import retrofit2.Response
import java.io.IOException

suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Resource<T> = try {
    val r = call()
    if (r.isSuccessful) {
        val body = r.body()
        when {
            body != null -> Resource.Success(body)
            r.code() == 204 -> {
                @Suppress("UNCHECKED_CAST")
                Resource.Success(Unit as T)
            }
            else -> Resource.Error("Respuesta vacía del servidor")
        }
    } else {
        Resource.Error(mapHttpError(r.code(), r.errorBody()?.string()))
    }
} catch (e: IOException) {
    Resource.Error("Sin conexión. Comprueba tu red e inténtalo de nuevo.")
} catch (e: JsonSyntaxException) {
    Resource.Error("Error al procesar la respuesta del servidor.")
} catch (e: Exception) {
    // CAL-04: en debug, relanzar excepciones no-red para detectar bugs de programación
    // (NullPointerException, IllegalStateException, etc.) en lugar de silenciarlos.
    if (BuildConfig.DEBUG) throw e
    Resource.Error("Error inesperado: ${e.localizedMessage}")
}

private fun mapHttpError(code: Int, body: String?): String {
    val apiDetail = body?.runCatching {
        org.json.JSONObject(this).getString("detail")
    }?.getOrNull()

    return apiDetail ?: when (code) {
        400  -> "Datos incorrectos. Revisa los campos e inténtalo de nuevo."
        401  -> "Sesión expirada. Vuelve a iniciar sesión."
        403  -> "No tienes permiso para realizar esta acción."
        404  -> "El recurso solicitado no existe."
        409  -> "Conflicto: ya existe un elemento con esos datos."
        422  -> "Datos no válidos. Revisa los campos obligatorios."
        429  -> "Demasiados intentos. Espera un momento e inténtalo de nuevo."
        500, 502, 503 -> "Error en el servidor. Inténtalo más tarde."
        else -> "Error inesperado ($code). Inténtalo de nuevo."
    }
}
