package com.thuo_ng.swift_chat_android.data.remote.api

import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationResponseDto
import retrofit2.Response
import retrofit2.http.GET

interface ConversationApi {
    @GET("api/v1/conversations")
    suspend fun getConversations(): Response<ConversationResponseDto>
}
