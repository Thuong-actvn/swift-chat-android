package com.thuo_ng.swift_chat_android.core.network

import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val code: Int? = null, val message: String) : NetworkResult<Nothing>()
}

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): NetworkResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                NetworkResult.Success(body)
            } else {
                NetworkResult.Error(response.code(), "Response body is null")
            }
        } else {
            val errorMsg = parseErrorBody(response.errorBody()?.string(), response.message())
            NetworkResult.Error(response.code(), errorMsg)
        }
    } catch (e: HttpException) {
        val errorMsg = parseErrorBody(e.response()?.errorBody()?.string(), e.message())
        NetworkResult.Error(e.code(), errorMsg)
    } catch (e: IOException) {
        NetworkResult.Error(null, "Network Error: ${e.message}")
    } catch (e: Exception) {
        NetworkResult.Error(null, "Unknown Error: ${e.message}")
    }
}

private fun parseErrorBody(errorBodyString: String?, defaultMessage: String): String {
    if (errorBodyString.isNullOrEmpty()) return defaultMessage
    return try {
        val jsonObject = org.json.JSONObject(errorBodyString)
        jsonObject.optString("message", jsonObject.optString("error", defaultMessage))
    } catch (e: Exception) {
        defaultMessage
    }
}
