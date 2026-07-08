package com.thuo_ng.swift_chat_android.domain.model

data class PublicUserProfile(
    val id: String,
    val handle: String,
    val displayName: String?,
    val avatarUrl: String?,
    val coverUrl: String?,
    val bio: String?,
    val website: String?,
    val location: String?,
    val isOnline: Boolean?,
    val lastSeen: String?,
    val createdAt: String
) {
    val displayLabel: String
        get() = displayName?.takeIf { it.isNotBlank() } ?: handle
}

data class SearchUser(
    val id: String,
    val handle: String,
    val displayName: String?,
    val avatarUrl: String?,
    val isFriend: Boolean?,
    val friendRequestStatus: String?
) {
    val displayLabel: String
        get() = displayName?.takeIf { it.isNotBlank() } ?: handle
}

data class FriendRequest(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val status: String,
    val createdAt: String,
    val sender: FriendUser,
    val receiver: FriendUser
)

data class FriendUser(
    val id: String,
    val handle: String,
    val displayName: String?,
    val avatarUrl: String?
) {
    val displayLabel: String
        get() = displayName?.takeIf { it.isNotBlank() } ?: handle
}

data class BlockedUser(
    val id: String,
    val handle: String,
    val displayName: String?,
    val avatarUrl: String?,
    val blockedAt: String
) {
    val displayLabel: String
        get() = displayName?.takeIf { it.isNotBlank() } ?: handle
}

data class OffsetPage<T>(
    val data: List<T>,
    val total: Int,
    val limit: Int,
    val offset: Int
) {
    val hasMore: Boolean
        get() = offset + data.size < total

    val nextOffset: Int
        get() = offset + data.size
}

data class SendFriendRequestResult(
    val result: String,
    val friendRequest: FriendRequestBase
)

data class FriendRequestBase(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val status: String,
    val createdAt: String
)
