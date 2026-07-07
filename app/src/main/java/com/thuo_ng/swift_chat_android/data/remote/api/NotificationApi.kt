package com.thuo_ng.swift_chat_android.data.remote.api

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {
    @GET("api/v1/notifications")
    suspend fun getNotifications(
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<JsonElement>

    @GET("api/v1/notifications/unread-count")
    suspend fun getUnreadCount(): Response<JsonElement>

    @PATCH("api/v1/notifications/read-all")
    suspend fun markAllAsRead(): Response<JsonElement>

    @PATCH("api/v1/notifications/{id}/read")
    suspend fun markAsRead(
        @Path("id") id: String
    ): Response<JsonElement>
}