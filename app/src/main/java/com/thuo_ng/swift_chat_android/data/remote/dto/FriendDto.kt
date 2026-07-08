package com.thuo_ng.swift_chat_android.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.thuo_ng.swift_chat_android.domain.model.BlockedUser
import com.thuo_ng.swift_chat_android.domain.model.FriendRequest
import com.thuo_ng.swift_chat_android.domain.model.FriendRequestBase
import com.thuo_ng.swift_chat_android.domain.model.FriendUser
import com.thuo_ng.swift_chat_android.domain.model.OffsetPage
import com.thuo_ng.swift_chat_android.domain.model.PublicUserProfile
import com.thuo_ng.swift_chat_android.domain.model.SearchUser
import com.thuo_ng.swift_chat_android.domain.model.SendFriendRequestResult

data class PublicUserProfileDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("handle")
    val handle: String,
    @SerializedName("displayName")
    val displayName: String?,
    @SerializedName("avatarUrl")
    val avatarUrl: String?,
    @SerializedName("coverUrl")
    val coverUrl: String?,
    @SerializedName("bio")
    val bio: String?,
    @SerializedName("website")
    val website: String?,
    @SerializedName("location")
    val location: String?,
    @SerializedName("isOnline")
    val isOnline: Boolean?,
    @SerializedName("lastSeen")
    val lastSeen: String?,
    @SerializedName("createdAt")
    val createdAt: String?
)

data class SearchUserDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("handle")
    val handle: String,
    @SerializedName("displayName")
    val displayName: String?,
    @SerializedName("avatarUrl")
    val avatarUrl: String?,
    @SerializedName("isFriend")
    val isFriend: Boolean?,
    @SerializedName("friendRequestStatus")
    val friendRequestStatus: String?
)

data class FriendRequestDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("senderId")
    val senderId: String,
    @SerializedName("receiverId")
    val receiverId: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("createdAt")
    val createdAt: String?,
    @SerializedName("sender")
    val sender: FriendUserDto,
    @SerializedName("receiver")
    val receiver: FriendUserDto
)

data class FriendUserDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("handle")
    val handle: String,
    @SerializedName("displayName")
    val displayName: String?,
    @SerializedName("avatarUrl")
    val avatarUrl: String?
)

data class BlockedUserDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("handle")
    val handle: String,
    @SerializedName("displayName")
    val displayName: String?,
    @SerializedName("avatarUrl")
    val avatarUrl: String?,
    @SerializedName("blockedAt")
    val blockedAt: String?
)

data class OffsetPageDto<T>(
    @SerializedName("data")
    val data: List<T>,
    @SerializedName("total")
    val total: Int,
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("offset")
    val offset: Int
)

data class SendFriendRequestBody(
    @SerializedName("receiverId")
    val receiverId: String
)

data class FriendRequestActionBody(
    @SerializedName("action")
    val action: String
)

data class SendFriendRequestResponseDto(
    @SerializedName("result")
    val result: String,
    @SerializedName("friendRequest")
    val friendRequest: FriendRequestBaseDto
)

data class FriendRequestBaseDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("senderId")
    val senderId: String,
    @SerializedName("receiverId")
    val receiverId: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("createdAt")
    val createdAt: String?
)

data class FriendRequestActionResponseDto(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("action")
    val action: String?,
    @SerializedName("requestId")
    val requestId: String?
)

data class SimpleSuccessResponseDto(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String?
)

data class BlockResponseDto(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("blocked")
    val blocked: Boolean,
    @SerializedName("targetUserId")
    val targetUserId: String
)

fun PublicUserProfileDto.toDomain(): PublicUserProfile =
    PublicUserProfile(
        id = id,
        handle = handle,
        displayName = displayName,
        avatarUrl = avatarUrl,
        coverUrl = coverUrl,
        bio = bio,
        website = website,
        location = location,
        isOnline = isOnline,
        lastSeen = lastSeen,
        createdAt = createdAt.orEmpty()
    )

fun SearchUserDto.toDomain(): SearchUser =
    SearchUser(
        id = id,
        handle = handle,
        displayName = displayName,
        avatarUrl = avatarUrl,
        isFriend = isFriend,
        friendRequestStatus = friendRequestStatus
    )

fun FriendRequestDto.toDomain(): FriendRequest =
    FriendRequest(
        id = id,
        senderId = senderId,
        receiverId = receiverId,
        status = status,
        createdAt = createdAt.orEmpty(),
        sender = sender.toDomain(),
        receiver = receiver.toDomain()
    )

fun FriendUserDto.toDomain(): FriendUser =
    FriendUser(
        id = id,
        handle = handle,
        displayName = displayName,
        avatarUrl = avatarUrl
    )

fun BlockedUserDto.toDomain(): BlockedUser =
    BlockedUser(
        id = id,
        handle = handle,
        displayName = displayName,
        avatarUrl = avatarUrl,
        blockedAt = blockedAt.orEmpty()
    )

fun FriendRequestBaseDto.toDomain(): FriendRequestBase =
    FriendRequestBase(
        id = id,
        senderId = senderId,
        receiverId = receiverId,
        status = status,
        createdAt = createdAt.orEmpty()
    )

fun SendFriendRequestResponseDto.toDomain(): SendFriendRequestResult =
    SendFriendRequestResult(
        result = result,
        friendRequest = friendRequest.toDomain()
    )

fun OffsetPageDto<PublicUserProfileDto>.toPublicUserProfilePage(): OffsetPage<PublicUserProfile> =
    OffsetPage(
        data = data.map { it.toDomain() },
        total = total,
        limit = limit,
        offset = offset
    )
