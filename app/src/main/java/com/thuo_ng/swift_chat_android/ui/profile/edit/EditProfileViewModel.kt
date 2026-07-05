package com.thuo_ng.swift_chat_android.ui.profile.edit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thuo_ng.swift_chat_android.data.remote.dto.UpdateProfileDto
import com.thuo_ng.swift_chat_android.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _effect = Channel<EditProfileEffect>(Channel.BUFFERED)
    val effect: Flow<EditProfileEffect> = _effect.receiveAsFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val user = userRepository.observeCurrentUser().first()
            if (user != null) {
                _uiState.update {
                    it.copy(
                        displayName = user.displayName ?: "",
                        handle = user.handle,
                        bio = user.bio ?: "",
                        website = user.website ?: "",
                        location = user.location ?: "",
                        avatarUrl = user.avatarUrl,
                        coverUrl = user.coverUrl
                    )
                }
            }
        }
    }

    fun handleIntent(intent: EditProfileIntent) {
        when (intent) {
            is EditProfileIntent.DisplayNameChanged -> _uiState.update { it.copy(displayName = intent.value) }
            is EditProfileIntent.HandleChanged -> _uiState.update { it.copy(handle = intent.value) }
            is EditProfileIntent.BioChanged -> _uiState.update { it.copy(bio = intent.value) }
            is EditProfileIntent.WebsiteChanged -> _uiState.update { it.copy(website = intent.value) }
            is EditProfileIntent.LocationChanged -> _uiState.update { it.copy(location = intent.value) }
            is EditProfileIntent.AvatarSelected -> _uiState.update { it.copy(pendingAvatarUri = intent.uri) }
            is EditProfileIntent.CoverSelected -> _uiState.update { it.copy(pendingCoverUri = intent.uri) }
            is EditProfileIntent.SaveProfile -> saveProfile()
        }
    }

    private fun saveProfile() {
        val state = _uiState.value
        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            var finalAvatarUrl = state.avatarUrl
            var finalCoverUrl = state.coverUrl

            // Upload Avatar if changed
            if (state.pendingAvatarUri != null) {
                val avatarResult = userRepository.uploadFile(state.pendingAvatarUri, context)
                if (avatarResult.isSuccess) {
                    finalAvatarUrl = avatarResult.getOrNull()
                } else {
                    _uiState.update { it.copy(isSaving = false, error = "Failed to upload avatar") }
                    _effect.send(EditProfileEffect.ShowToast("Failed to upload avatar"))
                    return@launch
                }
            }

            // Upload Cover if changed
            if (state.pendingCoverUri != null) {
                val coverResult = userRepository.uploadFile(state.pendingCoverUri, context)
                if (coverResult.isSuccess) {
                    finalCoverUrl = coverResult.getOrNull()
                } else {
                    _uiState.update { it.copy(isSaving = false, error = "Failed to upload cover") }
                    _effect.send(EditProfileEffect.ShowToast("Failed to upload cover"))
                    return@launch
                }
            }

            // Update Profile
            val updateDto = UpdateProfileDto(
                handle = state.handle.ifBlank { null },
                displayName = state.displayName.ifBlank { null },
                avatarUrl = finalAvatarUrl,
                coverUrl = finalCoverUrl,
                bio = state.bio.ifBlank { null },
                website = state.website.ifBlank { null },
                location = state.location.ifBlank { null }
            )

            val updateResult = userRepository.updateProfile(updateDto)
            if (updateResult.isSuccess) {
                _uiState.update { it.copy(isSaving = false) }
                _effect.send(EditProfileEffect.ShowToast("updated successfully"))
                _effect.send(EditProfileEffect.NavigateBack)
            } else {
                _uiState.update { it.copy(isSaving = false, error = updateResult.exceptionOrNull()?.message) }
                _effect.send(EditProfileEffect.ShowToast("Error update: ${updateResult.exceptionOrNull()?.message}"))
            }
        }
    }
}
