package com.thuo_ng.swift_chat_android.data.remote.api

import com.thuo_ng.swift_chat_android.data.remote.dto.BlockResponseDto
import com.thuo_ng.swift_chat_android.data.remote.dto.BlockedUserDto
import com.thuo_ng.swift_chat_android.data.remote.dto.FriendRequestActionBody
import com.thuo_ng.swift_chat_android.data.remote.dto.FriendRequestActionResponseDto
import com.thuo_ng.swift_chat_android.data.remote.dto.FriendRequestDto
import com.thuo_ng.swift_chat_android.data.remote.dto.OffsetPageDto
import com.thuo_ng.swift_chat_android.data.remote.dto.PublicUserProfileDto
import com.thuo_ng.swift_chat_android.data.remote.dto.SendFriendRequestBody
import com.thuo_ng.swift_chat_android.data.remote.dto.SendFriendRequestResponseDto
import com.thuo_ng.swift_chat_android.data.remote.dto.SimpleSuccessResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FriendApi {
    @GET("api/v1/friends")
    suspend fun getFriends(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<OffsetPageDto<PublicUserProfileDto>>

    @POST("api/v1/friend-requests")
    suspend fun sendFriendRequest(
        @Body body: SendFriendRequestBody
    ): Response<SendFriendRequestResponseDto>

    @GET("api/v1/friend-requests")
    suspend fun getFriendRequests(): Response<List<FriendRequestDto>>

    @PATCH("api/v1/friend-requests/{requestId}")
    suspend fun respondToFriendRequest(
        @Path("requestId") requestId: String,
        @Body body: FriendRequestActionBody
    ): Response<FriendRequestActionResponseDto>

    @DELETE("api/v1/friend-requests/{requestId}")
    suspend fun cancelFriendRequest(
        @Path("requestId") requestId: String
    ): Response<SimpleSuccessResponseDto>

    @DELETE("api/v1/friends/{accountId}")
    suspend fun removeFriend(
        @Path("accountId") accountId: String
    ): Response<SimpleSuccessResponseDto>

    @POST("api/v1/block/{targetUserId}")
    suspend fun blockUser(
        @Path("targetUserId") targetUserId: String
    ): Response<BlockResponseDto>

    @DELETE("api/v1/block/{targetUserId}")
    suspend fun unblockUser(
        @Path("targetUserId") targetUserId: String
    ): Response<BlockResponseDto>

    @GET("api/v1/block")
    suspend fun getBlockedUsers(): Response<List<BlockedUserDto>>
}
