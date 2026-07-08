package com.thuo_ng.swift_chat_android.data.remote.api

import com.google.gson.JsonElement
import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationResponseDto
import com.thuo_ng.swift_chat_android.data.remote.dto.CreateConversationRequestDto
import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationMemberDto
import retrofit2.http.Body
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface ConversationApi {
    @GET("api/v1/conversations")
    suspend fun getConversations(
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null,
        @Query("q") query: String? = null
    ): Response<ConversationResponseDto>

    @GET("api/v1/conversations/{conversationId}/members")
    suspend fun getConversationMembers(
        @Path("conversationId") conversationId: String
    ): Response<List<ConversationMemberDto>>

    @POST("api/v1/conversations")
    suspend fun createConversation(
        @Body request: CreateConversationRequestDto
    ): Response<JsonElement>
}
