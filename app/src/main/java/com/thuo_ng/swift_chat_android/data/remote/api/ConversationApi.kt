package com.thuo_ng.swift_chat_android.data.remote.api

import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationResponseDto
import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationMemberDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ConversationApi {
    @GET("api/v1/conversations")
    suspend fun getConversations(): Response<ConversationResponseDto>

    @GET("api/v1/conversations/{conversationId}/members")
    suspend fun getConversationMembers(
        @Path("conversationId") conversationId: String
    ): Response<List<ConversationMemberDto>>
}
