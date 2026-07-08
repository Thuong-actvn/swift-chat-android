package com.thuo_ng.swift_chat_android.ui.userprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.domain.repository.ConversationRepository
import com.thuo_ng.swift_chat_android.domain.repository.FriendRepository
import com.thuo_ng.swift_chat_android.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private val _effect = Channel<UserProfileEffect>(Channel.BUFFERED)
    val effect: Flow<UserProfileEffect> = _effect.receiveAsFlow()

    private var currentUserId: String? = null
    private var statsJob: Job? = null

    init {
        viewModelScope.launch {
            userRepository.observeCurrentUser().collect { user ->
                currentUserId = user?.id
                val loadedUser = _uiState.value.user
                if (loadedUser != null) {
                    val isMe = loadedUser.id == currentUserId
                    _uiState.update { it.copy(isCurrentUser = isMe) }
                    if (isMe) startFetchingMyStats()
                }
            }
        }
    }

    fun handleIntent(intent: UserProfileIntent) {
        when (intent) {
            is UserProfileIntent.Load -> loadUser(intent.userId)
            is UserProfileIntent.TabSelected -> _uiState.update { it.copy(selectedTab = intent.tab) }
            is UserProfileIntent.MessageClicked -> openChat()
            is UserProfileIntent.AudioCallClicked -> { /* TODO: Audio call */ }
            is UserProfileIntent.VideoCallClicked -> { /* TODO: Video call */ }
            is UserProfileIntent.MoreClicked -> { /* TODO: More options */ }
            UserProfileIntent.EditProfileClicked -> {
                viewModelScope.launch {
                    _effect.send(UserProfileEffect.NavigateToEditProfile)
                }
            }
        }
    }

    private fun loadUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = friendRepository.getPublicProfile(userId)
            when (result) {
                is NetworkResult.Success -> {
                    val isMe = userId == currentUserId
                    _uiState.update {
                        it.copy(
                            user = result.data,
                            isLoading = false,
                            isCurrentUser = isMe
                        )
                    }
                    if (isMe) {
                        startFetchingMyStats()
                    } else {
                        _uiState.update { it.copy(friendCount = 0, groupCount = 0, mediaCount = 0) }
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    private fun startFetchingMyStats() {
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            launch {
                conversationRepository.observeConversations().collect { convs ->
                    val groupsCount = convs.count { it.type.equals("group", ignoreCase = true) }
                    _uiState.update { it.copy(groupCount = groupsCount) }
                }
            }
            launch {
                val friendsResult = friendRepository.getFriends(limit = 1, offset = 0)
                if (friendsResult is NetworkResult.Success) {
                    _uiState.update { it.copy(friendCount = friendsResult.data.total) }
                }
            }
            _uiState.update { it.copy(mediaCount = 0) }
        }
    }

    private fun openChat() {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            val result = conversationRepository.findDirectConversationWith(user.id)
            when (result) {
                is NetworkResult.Success -> {
                    val conv = result.data
                    if (conv != null) {
                        _effect.send(UserProfileEffect.NavigateToChat(conv.id))
                    } else {
                        _effect.send(
                            UserProfileEffect.NavigateToPendingChat(
                                partnerId = user.id,
                                displayName = user.displayLabel,
                                avatarUrl = user.avatarUrl
                            )
                        )
                    }
                }
                else -> {
                    _effect.send(
                        UserProfileEffect.NavigateToPendingChat(
                            partnerId = user.id,
                            displayName = user.displayLabel,
                            avatarUrl = user.avatarUrl
                        )
                    )
                }
            }
        }
    }
}
