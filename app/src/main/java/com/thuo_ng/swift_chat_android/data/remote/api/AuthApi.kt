package com.thuo_ng.swift_chat_android.data.remote.api

import com.thuo_ng.swift_chat_android.data.remote.dto.AuthResponseDto
import com.thuo_ng.swift_chat_android.data.remote.dto.DeviceTokenRequest
import com.thuo_ng.swift_chat_android.data.remote.dto.GoogleLoginRequest
import com.thuo_ng.swift_chat_android.data.remote.dto.SignInRequest
import com.thuo_ng.swift_chat_android.data.remote.dto.RefreshTokenRequest
import com.thuo_ng.swift_chat_android.data.remote.dto.RefreshTokenResponseDto
import com.thuo_ng.swift_chat_android.data.remote.dto.SignupRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun signIn(
        @Body request: SignInRequest
    ): Response<AuthResponseDto>

    @POST("api/v1/auth/register")
    suspend fun signup(
        @Body request: SignupRequest
    ): Response<AuthResponseDto>

    @POST("api/v1/auth/refresh-token")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<RefreshTokenResponseDto>

    @POST("api/v1/auth/google")
    suspend fun googleLogin(
        @Body request: GoogleLoginRequest
    ): Response<AuthResponseDto>

    @POST("api/v1/auth/device-token")
    suspend fun registerDeviceToken(
        @Body request: DeviceTokenRequest
    ): Response<Unit>
}
