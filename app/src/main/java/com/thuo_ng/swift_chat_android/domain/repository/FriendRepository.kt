package com.thuo_ng.swift_chat_android.domain.repository

import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.domain.model.BlockedUser
import com.thuo_ng.swift_chat_android.domain.model.FriendRequest
import com.thuo_ng.swift_chat_android.domain.model.OffsetPage
import com.thuo_ng.swift_chat_android.domain.model.PublicUserProfile
import com.thuo_ng.swift_chat_android.domain.model.SearchUser
import com.thuo_ng.swift_chat_android.domain.model.SendFriendRequestResult

interface FriendRepository {
    suspend fun getFriends(limit: Int = 20, offset: Int = 0): NetworkResult<OffsetPage<PublicUserProfile>>
    suspend fun getPublicProfile(userId: String): NetworkResult<PublicUserProfile>
    suspend fun searchUsers(query: String, scope: String = "all"): NetworkResult<List<SearchUser>>
    suspend fun getFriendRequests(): NetworkResult<List<FriendRequest>>
    suspend fun sendFriendRequest(receiverId: String): NetworkResult<SendFriendRequestResult>
    suspend fun acceptFriendRequest(requestId: String): NetworkResult<Unit>
    suspend fun rejectFriendRequest(requestId: String): NetworkResult<Unit>
    suspend fun cancelFriendRequest(requestId: String): NetworkResult<Unit>
    suspend fun removeFriend(accountId: String): NetworkResult<Unit>
    suspend fun getBlockedUsers(): NetworkResult<List<BlockedUser>>
    suspend fun blockUser(targetUserId: String): NetworkResult<Unit>
    suspend fun unblockUser(targetUserId: String): NetworkResult<Unit>
}
