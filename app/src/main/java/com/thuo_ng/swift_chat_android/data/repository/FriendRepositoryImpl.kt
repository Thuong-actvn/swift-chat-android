package com.thuo_ng.swift_chat_android.data.repository

import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.core.network.safeApiCall
import com.thuo_ng.swift_chat_android.data.remote.api.FriendApi
import com.thuo_ng.swift_chat_android.data.remote.api.UserApi
import com.thuo_ng.swift_chat_android.data.remote.dto.FriendRequestActionBody
import com.thuo_ng.swift_chat_android.data.remote.dto.SendFriendRequestBody
import com.thuo_ng.swift_chat_android.data.remote.dto.toDomain
import com.thuo_ng.swift_chat_android.data.remote.dto.toPublicUserProfilePage
import com.thuo_ng.swift_chat_android.domain.model.BlockedUser
import com.thuo_ng.swift_chat_android.domain.model.FriendRequest
import com.thuo_ng.swift_chat_android.domain.model.OffsetPage
import com.thuo_ng.swift_chat_android.domain.model.PublicUserProfile
import com.thuo_ng.swift_chat_android.domain.model.SearchUser
import com.thuo_ng.swift_chat_android.domain.model.SendFriendRequestResult
import com.thuo_ng.swift_chat_android.domain.repository.FriendRepository
import javax.inject.Inject

class FriendRepositoryImpl @Inject constructor(
    private val friendApi: FriendApi,
    private val userApi: UserApi
) : FriendRepository {

    override suspend fun getFriends(
        limit: Int,
        offset: Int
    ): NetworkResult<OffsetPage<PublicUserProfile>> {
        return when (val result = safeApiCall { friendApi.getFriends(limit = limit, offset = offset) }) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toPublicUserProfilePage())
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun getPublicProfile(userId: String): NetworkResult<PublicUserProfile> {
        return when (val result = safeApiCall { userApi.getPublicProfile(userId) }) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun searchUsers(
        query: String,
        scope: String
    ): NetworkResult<List<SearchUser>> {
        return when (val result = safeApiCall { userApi.searchUsers(query = query, scope = scope) }) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.map { it.toDomain() })
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun getFriendRequests(): NetworkResult<List<FriendRequest>> {
        return when (val result = safeApiCall { friendApi.getFriendRequests() }) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.map { it.toDomain() })
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun sendFriendRequest(receiverId: String): NetworkResult<SendFriendRequestResult> {
        return when (val result = safeApiCall {
            friendApi.sendFriendRequest(SendFriendRequestBody(receiverId = receiverId))
        }) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun acceptFriendRequest(requestId: String): NetworkResult<Unit> {
        return respondToFriendRequest(requestId = requestId, action = "accepted")
    }

    override suspend fun rejectFriendRequest(requestId: String): NetworkResult<Unit> {
        return respondToFriendRequest(requestId = requestId, action = "rejected")
    }

    override suspend fun cancelFriendRequest(requestId: String): NetworkResult<Unit> {
        return when (val result = safeApiCall { friendApi.cancelFriendRequest(requestId) }) {
            is NetworkResult.Success -> {
                if (result.data.success) NetworkResult.Success(Unit)
                else NetworkResult.Error(message = result.data.message ?: "Could not cancel friend request")
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun removeFriend(accountId: String): NetworkResult<Unit> {
        return when (val result = safeApiCall { friendApi.removeFriend(accountId) }) {
            is NetworkResult.Success -> {
                if (result.data.success) NetworkResult.Success(Unit)
                else NetworkResult.Error(message = result.data.message ?: "Could not remove friend")
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun getBlockedUsers(): NetworkResult<List<BlockedUser>> {
        return when (val result = safeApiCall { friendApi.getBlockedUsers() }) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.map { it.toDomain() })
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun blockUser(targetUserId: String): NetworkResult<Unit> {
        return when (val result = safeApiCall { friendApi.blockUser(targetUserId) }) {
            is NetworkResult.Success -> {
                if (result.data.success && result.data.blocked) NetworkResult.Success(Unit)
                else NetworkResult.Error(message = "Could not block this user")
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun unblockUser(targetUserId: String): NetworkResult<Unit> {
        return when (val result = safeApiCall { friendApi.unblockUser(targetUserId) }) {
            is NetworkResult.Success -> {
                if (result.data.success && !result.data.blocked) NetworkResult.Success(Unit)
                else NetworkResult.Error(message = "Could not unblock this user")
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    private suspend fun respondToFriendRequest(
        requestId: String,
        action: String
    ): NetworkResult<Unit> {
        return when (val result = safeApiCall {
            friendApi.respondToFriendRequest(
                requestId = requestId,
                body = FriendRequestActionBody(action = action)
            )
        }) {
            is NetworkResult.Success -> {
                if (result.data.success) NetworkResult.Success(Unit)
                else NetworkResult.Error(message = "Could not update friend request")
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }
}
