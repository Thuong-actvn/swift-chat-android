package com.thuo_ng.swift_chat_android.ui.friends

import com.thuo_ng.swift_chat_android.domain.model.BlockedUser
import com.thuo_ng.swift_chat_android.domain.model.FriendRequest
import com.thuo_ng.swift_chat_android.domain.model.PublicUserProfile
import com.thuo_ng.swift_chat_android.domain.model.SearchUser

enum class FriendsSection {
    Friends,
    Received,
    Sent,
    Blocked
}

data class FriendsUiState(
    val selectedSection: FriendsSection = FriendsSection.Friends,
    val myAccountId: String? = null,
    val friends: List<PublicUserProfile> = emptyList(),
    val totalFriends: Int = 0,
    val friendsLimit: Int = 20,
    val nextFriendsOffset: Int = 0,
    val hasMoreFriends: Boolean = false,
    val incomingRequests: List<FriendRequest> = emptyList(),
    val outgoingRequests: List<FriendRequest> = emptyList(),
    val blockedUsers: List<BlockedUser> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<SearchUser> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMoreFriends: Boolean = false,
    val isSearching: Boolean = false,
    val actionKeysInProgress: Set<String> = emptySet(),
    val errorMessage: String? = null
) {
    val requestBadgeCount: Int
        get() = incomingRequests.size

    val isSearchMode: Boolean
        get() = searchQuery.trim().isNotEmpty()

    fun outgoingRequestFor(accountId: String): FriendRequest? =
        outgoingRequests.firstOrNull { it.receiverId == accountId }

    fun incomingRequestFor(accountId: String): FriendRequest? =
        incomingRequests.firstOrNull { it.senderId == accountId }

    fun isActionLoading(actionKey: String): Boolean =
        actionKeysInProgress.contains(actionKey)
}

sealed interface FriendsIntent {
    data object Refresh : FriendsIntent
    data object LoadMoreFriends : FriendsIntent
    data class SectionSelected(val section: FriendsSection) : FriendsIntent
    data class SearchQueryChanged(val query: String) : FriendsIntent
    data class SendFriendRequest(val accountId: String) : FriendsIntent
    data class AcceptFriendRequest(val requestId: String) : FriendsIntent
    data class RejectFriendRequest(val requestId: String) : FriendsIntent
    data class CancelFriendRequest(val requestId: String) : FriendsIntent
    data class RemoveFriend(val accountId: String) : FriendsIntent
    data class BlockUser(val accountId: String) : FriendsIntent
    data class UnblockUser(val accountId: String) : FriendsIntent
    data class UserSelected(
        val accountId: String,
        val displayName: String,
        val avatarUrl: String?,
        val isFriend: Boolean
    ) : FriendsIntent
}

sealed interface FriendsEffect {
    data class ShowMessage(val message: String) : FriendsEffect
    data class OpenConversation(val conversationId: String) : FriendsEffect
    data class OpenPendingDirectChat(
        val partnerId: String,
        val displayName: String,
        val avatarUrl: String?
    ) : FriendsEffect
    data class OpenPublicProfile(val accountId: String, val displayName: String) : FriendsEffect
}

object FriendActionKeys {
    fun send(accountId: String) = "send:$accountId"
    fun accept(requestId: String) = "accept:$requestId"
    fun reject(requestId: String) = "reject:$requestId"
    fun cancel(requestId: String) = "cancel:$requestId"
    fun remove(accountId: String) = "remove:$accountId"
    fun block(accountId: String) = "block:$accountId"
    fun unblock(accountId: String) = "unblock:$accountId"
    fun open(accountId: String) = "open:$accountId"
}
