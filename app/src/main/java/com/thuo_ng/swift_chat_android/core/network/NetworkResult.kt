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
            NetworkResult.Error(response.code(), response.message())
        }
    } catch (e: HttpException) {
        NetworkResult.Error(e.code(), e.message() ?: "HTTP Exception")
    } catch (e: IOException) {
        NetworkResult.Error(null, "Network Error: ${e.message}")
    } catch (e: Exception) {
        NetworkResult.Error(null, "Unknown Error: ${e.message}")
    }
}
