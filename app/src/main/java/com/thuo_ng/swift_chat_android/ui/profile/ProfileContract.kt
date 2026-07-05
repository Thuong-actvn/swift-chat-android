package com.thuo_ng.swift_chat_android.ui.profile

import com.thuo_ng.swift_chat_android.domain.model.User

// State
data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true
)

// Intent
sealed class ProfileIntent {
    object LogoutClicked : ProfileIntent()
    // TODO: Add intents for other settings menu clicks later
}

// Effect
sealed class ProfileEffect {
    object NavigateToEditProfile : ProfileEffect()
    object NavigateToLogin : ProfileEffect()
}
