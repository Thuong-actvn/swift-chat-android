package com.thuo_ng.swift_chat_android.data.remote.api

import com.google.gson.JsonElement
import com.thuo_ng.swift_chat_android.data.remote.dto.AddConversationMembersRequestDto
import com.thuo_ng.swift_chat_android.data.remote.dto.ChangeMemberRoleRequestDto
import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationActionResponseDto
import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationResponseDto
import com.thuo_ng.swift_chat_android.data.remote.dto.CreateConversationRequestDto
import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationMemberDto
import com.thuo_ng.swift_chat_android.data.remote.dto.MuteConversationRequestDto
import com.thuo_ng.swift_chat_android.data.remote.dto.TransferLeadershipRequestDto
import com.thuo_ng.swift_chat_android.data.remote.dto.UpdateGroupInfoRequestDto
import retrofit2.http.Body
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
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

    @PATCH("api/v1/conversations/{conversationId}")
    suspend fun updateGroupInfo(
        @Path("conversationId") conversationId: String,
        @Body request: UpdateGroupInfoRequestDto
    ): Response<JsonElement>

    @DELETE("api/v1/conversations/{conversationId}")
    suspend fun deleteConversation(
        @Path("conversationId") conversationId: String
    ): Response<ConversationActionResponseDto>

    @POST("api/v1/conversations/{conversationId}/members")
    suspend fun addMembers(
        @Path("conversationId") conversationId: String,
        @Body request: AddConversationMembersRequestDto
    ): Response<ConversationActionResponseDto>

    @DELETE("api/v1/conversations/{conversationId}/members/me")
    suspend fun leaveGroup(
        @Path("conversationId") conversationId: String
    ): Response<ConversationActionResponseDto>

    @DELETE("api/v1/conversations/{conversationId}/members/{accountId}")
    suspend fun kickMember(
        @Path("conversationId") conversationId: String,
        @Path("accountId") accountId: String
    ): Response<ConversationActionResponseDto>

    @PATCH("api/v1/conversations/{conversationId}/members/{accountId}/role")
    suspend fun changeMemberRole(
        @Path("conversationId") conversationId: String,
        @Path("accountId") accountId: String,
        @Body request: ChangeMemberRoleRequestDto
    ): Response<ConversationActionResponseDto>

    @POST("api/v1/conversations/{conversationId}/transfer-leadership")
    suspend fun transferLeadership(
        @Path("conversationId") conversationId: String,
        @Body request: TransferLeadershipRequestDto
    ): Response<ConversationActionResponseDto>

    @POST("api/v1/conversations/{conversationId}/mute")
    suspend fun muteConversation(
        @Path("conversationId") conversationId: String,
        @Body request: MuteConversationRequestDto
    ): Response<ConversationActionResponseDto>

    @DELETE("api/v1/conversations/{conversationId}/mute")
    suspend fun unmuteConversation(
        @Path("conversationId") conversationId: String
    ): Response<ConversationActionResponseDto>
}
