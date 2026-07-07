package com.thuo_ng.swift_chat_android.data.remote.api

import com.thuo_ng.swift_chat_android.data.remote.dto.MessageDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MessageApi {
    @GET("api/v1/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String,
        @Query("limit") limit: Int = 50,
        @Query("cursor") cursor: String? = null
    ): Response<List<MessageDto>>
}
