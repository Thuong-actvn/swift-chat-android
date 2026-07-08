package com.thuo_ng.swift_chat_android.ui.userprofile

import com.thuo_ng.swift_chat_android.domain.model.PublicUserProfile

// ── State ────────────────────────────────────────────────
data class UserProfileUiState(
    val user: PublicUserProfile? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedTab: UserProfileTab = UserProfileTab.MEDIA,
    val isCurrentUser: Boolean = false,
    val friendCount: Int = 0,
    val groupCount: Int = 0,
    val mediaCount: Int = 0
)

enum class UserProfileTab(val label: String) {
    MEDIA("Media"),
    FILES("Files"),
    LINKS("Links")
}

// ── Intent ───────────────────────────────────────────────
sealed class UserProfileIntent {
    data class Load(val userId: String) : UserProfileIntent()
    data class TabSelected(val tab: UserProfileTab) : UserProfileIntent()
    object MessageClicked : UserProfileIntent()
    object AudioCallClicked : UserProfileIntent()
    object VideoCallClicked : UserProfileIntent()
    object MoreClicked : UserProfileIntent()
    object EditProfileClicked : UserProfileIntent()
}

// ── Effect ───────────────────────────────────────────────
sealed class UserProfileEffect {
    data class NavigateToChat(val conversationId: String) : UserProfileEffect()
    data class NavigateToPendingChat(
        val partnerId: String,
        val displayName: String,
        val avatarUrl: String?
    ) : UserProfileEffect()
    object NavigateBack : UserProfileEffect()
    object NavigateToEditProfile : UserProfileEffect()
}
