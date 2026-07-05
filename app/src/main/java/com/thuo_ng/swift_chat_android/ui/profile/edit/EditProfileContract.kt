package com.thuo_ng.swift_chat_android.ui.profile.edit

import android.net.Uri

// State
data class EditProfileUiState(
    val displayName: String = "",
    val handle: String = "",
    val bio: String = "",
    val website: String = "",
    val location: String = "",
    val avatarUrl: String? = null,
    val pendingAvatarUri: Uri? = null,
    val coverUrl: String? = null,
    val pendingCoverUri: Uri? = null,
    val isSaving: Boolean = false,
    val error: String? = null
)

// Intent
sealed class EditProfileIntent {
    data class DisplayNameChanged(val value: String) : EditProfileIntent()
    data class HandleChanged(val value: String) : EditProfileIntent()
    data class BioChanged(val value: String) : EditProfileIntent()
    data class WebsiteChanged(val value: String) : EditProfileIntent()
    data class LocationChanged(val value: String) : EditProfileIntent()
    data class AvatarSelected(val uri: Uri) : EditProfileIntent()
    data class CoverSelected(val uri: Uri) : EditProfileIntent()
    object SaveProfile : EditProfileIntent()
}

// Effect
sealed class EditProfileEffect {
    object NavigateBack : EditProfileEffect()
    data class ShowToast(val message: String) : EditProfileEffect()
}
