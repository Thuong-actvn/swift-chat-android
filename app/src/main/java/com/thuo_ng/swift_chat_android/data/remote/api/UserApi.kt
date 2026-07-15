package com.thuo_ng.swift_chat_android.data.remote.api

import com.thuo_ng.swift_chat_android.data.remote.dto.UpdateProfileDto
import com.thuo_ng.swift_chat_android.data.remote.dto.UploadResponseDto
import com.thuo_ng.swift_chat_android.data.remote.dto.UserResponseDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface UserApi {
    @GET("api/v1/users/me")
    suspend fun getCurrentUser(): UserResponseDto

    @PATCH("api/v1/users/me")
    suspend fun updateProfile(
        @Body request: UpdateProfileDto
    ): UserResponseDto

    @Multipart
    @POST("api/v1/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): Response<UploadResponseDto>
}