package com.thuo_ng.swift_chat_android.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thuo_ng.swift_chat_android.domain.repository.AuthRepository
import com.thuo_ng.swift_chat_android.domain.repository.ConversationRepository
import com.thuo_ng.swift_chat_android.domain.repository.FriendRepository
import com.thuo_ng.swift_chat_android.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val friendRepository: FriendRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ProfileEffect>(Channel.BUFFERED)
    val effect: Flow<ProfileEffect> = _effect.receiveAsFlow()

    init {
        observeUserAndStats()
    }

    private fun observeUserAndStats() {
        viewModelScope.launch {
            // Lắng nghe dữ liệu user và thống kê
            launch {
                combine(
                    userRepository.observeCurrentUser(),
                    conversationRepository.observeConversations()
                ) { user, conversations ->
                    val groupsCount = conversations.count { it.type.equals("group", ignoreCase = true) }
                    Triple(user, groupsCount, 0) // mediaCount placeholder = 0
                }.collect { (user, groupsCount, mediaCount) ->
                    _uiState.update { 
                        it.copy(
                            user = user, 
                            isLoading = user == null,
                            groupCount = groupsCount,
                            mediaCount = mediaCount
                        ) 
                    }
                }
            }
            
            // Fetch friend count and sync user
            launch {
                try {
                    userRepository.syncCurrentUser()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                val friendsResult = friendRepository.getFriends(limit = 1, offset = 0)
                if (friendsResult is com.thuo_ng.swift_chat_android.core.network.NetworkResult.Success) {
                    _uiState.update { it.copy(friendCount = friendsResult.data.total) }
                }

                if (_uiState.value.user == null) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.LogoutClicked -> logout()
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            userRepository.clearCurrentUser()
            authRepository.logout()
            _effect.send(ProfileEffect.NavigateToLogin)
        }
    }
}
