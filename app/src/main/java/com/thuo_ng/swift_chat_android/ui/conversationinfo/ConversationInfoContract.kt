package com.thuo_ng.swift_chat_android.ui.conversationinfo

import com.thuo_ng.swift_chat_android.domain.model.Conversation
import com.thuo_ng.swift_chat_android.domain.model.ConversationMember
import com.thuo_ng.swift_chat_android.domain.model.MuteDuration
import com.thuo_ng.swift_chat_android.domain.model.PublicUserProfile
import com.thuo_ng.swift_chat_android.domain.model.SearchUser

data class ConversationInfoUiState(
    val conversationId: String = "",
    val conversation: Conversation? = null,
    val members: List<ConversationMember> = emptyList(),
    val currentAccountId: String? = null,
    val friends: List<PublicUserProfile> = emptyList(),
    val addMemberSearchQuery: String = "",
    val addMemberSearchResults: List<SearchUser> = emptyList(),
    val selectedAddMemberIds: Set<String> = emptySet(),
    val mediaPreviews: List<ConversationMediaPreview> = emptyList(),
    val documentCount: Int = 0,
    val isLoading: Boolean = true,
    val isRefreshingMembers: Boolean = false,
    val isLoadingFriends: Boolean = false,
    val isSearchingAddMembers: Boolean = false,
    val actionKeysInProgress: Set<String> = emptySet(),
    val showMuteDurationSheet: Boolean = false,
    val showAddMembersSheet: Boolean = false,
    val showAllMembersSheet: Boolean = false,
    val allMembersSearchQuery: String = "",
    val errorMessage: String? = null
) {
    val isGroup: Boolean
        get() = conversation?.type.equals("group", ignoreCase = true)

    val isDirect: Boolean
        get() = conversation?.type.equals("direct", ignoreCase = true)

    val currentRole: String?
        get() = conversation?.currentParticipant?.role

    val memberCount: Int
        get() = if (isGroup) {
            members.size.takeIf { it > 0 } ?: conversation?.totalParticipants ?: 0
        } else {
            conversation?.totalParticipants ?: 2
        }

    val displayedMembers: List<ConversationMember>
        get() = members.take(3)

    fun isActionLoading(actionKey: String): Boolean =
        actionKeysInProgress.contains(actionKey)
}

data class AddMemberCandidate(
    val accountId: String,
    val handle: String,
    val displayName: String,
    val avatarUrl: String?,
    val isFriend: Boolean
)

data class ConversationMediaPreview(
    val url: String
)

sealed interface ConversationInfoIntent {
    data class Start(val conversationId: String) : ConversationInfoIntent
    data object Refresh : ConversationInfoIntent
    data class MuteSwitchChanged(val checked: Boolean) : ConversationInfoIntent
    data class MuteDurationSelected(val duration: MuteDuration) : ConversationInfoIntent
    data object DismissMuteDurationSheet : ConversationInfoIntent
    data object OpenAddMembers : ConversationInfoIntent
    data object DismissAddMembers : ConversationInfoIntent
    data class AddMemberSearchChanged(val query: String) : ConversationInfoIntent
    data class ToggleAddMemberSelection(val accountId: String) : ConversationInfoIntent
    data object SubmitAddMembers : ConversationInfoIntent
    data object ShowAllMembers : ConversationInfoIntent
    data object DismissAllMembers : ConversationInfoIntent
    data class AllMembersSearchChanged(val query: String) : ConversationInfoIntent
    data class KickMember(val accountId: String) : ConversationInfoIntent
    data class ChangeMemberRole(val accountId: String, val role: String) : ConversationInfoIntent
    data class TransferLeadership(val accountId: String) : ConversationInfoIntent
    data object LeaveGroup : ConversationInfoIntent
    data object DeleteConversation : ConversationInfoIntent
}

sealed interface ConversationInfoEffect {
    data class ShowMessage(val message: String) : ConversationInfoEffect
    data object ConversationClosed : ConversationInfoEffect
}

object ConversationInfoActionKeys {
    const val Refresh = "refresh"
    const val Mute = "mute"
    const val Unmute = "unmute"
    const val AddMembers = "add-members"
    const val LeaveGroup = "leave-group"
    const val DeleteConversation = "delete-conversation"

    fun kick(accountId: String) = "kick:$accountId"
    fun role(accountId: String) = "role:$accountId"
    fun transfer(accountId: String) = "transfer:$accountId"
}
