package com.thuo_ng.swift_chat_android.data.remote.api

import com.thuo_ng.swift_chat_android.data.remote.dto.LogoutRequest
import com.thuo_ng.swift_chat_android.data.remote.dto.PublicUserProfileDto
import com.thuo_ng.swift_chat_android.data.remote.dto.SearchUserDto
import com.thuo_ng.swift_chat_android.data.remote.dto.UpdateProfileDto
import com.thuo_ng.swift_chat_android.data.remote.dto.UploadResponseDto
import com.thuo_ng.swift_chat_android.data.remote.dto.UserResponseDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface UserApi {
    @GET("api/v1/users/me")
    suspend fun getCurrentUser(): UserResponseDto

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Body request: LogoutRequest
    ): Response<Unit>

    @PATCH("api/v1/users/me")
    suspend fun updateProfile(
        @Body request: UpdateProfileDto
    ): UserResponseDto

    @GET("api/v1/users/search")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("scope") scope: String = "all"
    ): Response<List<SearchUserDto>>

    @GET("api/v1/users/{accountId}")
    suspend fun getPublicProfile(
        @Path("accountId") accountId: String
    ): Response<PublicUserProfileDto>

    @GET("api/v1/users/handle/{handle}")
    suspend fun getPublicProfileByHandle(
        @Path("handle") handle: String
    ): Response<PublicUserProfileDto>

    @Multipart
    @POST("api/v1/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): Response<UploadResponseDto>
}
