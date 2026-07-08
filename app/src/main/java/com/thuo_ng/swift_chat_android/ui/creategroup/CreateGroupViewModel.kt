package com.thuo_ng.swift_chat_android.ui.creategroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.domain.repository.ConversationRepository
import com.thuo_ng.swift_chat_android.domain.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    private val _effect = Channel<CreateGroupEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadFriends()
    }

    fun handleIntent(intent: CreateGroupIntent) {
        when (intent) {
            is CreateGroupIntent.TitleChanged -> _uiState.update { it.copy(title = intent.title) }
            is CreateGroupIntent.SearchQueryChanged -> _uiState.update { it.copy(searchQuery = intent.query) }
            is CreateGroupIntent.UserSelected -> toggleUserSelection(intent.userId)
            CreateGroupIntent.CreateClicked -> createGroup()
            CreateGroupIntent.Refresh -> loadFriends()
        }
    }

    private fun loadFriends() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = friendRepository.getFriends(limit = 100, offset = 0)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(friends = result.data.data, isLoading = false) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    private fun toggleUserSelection(userId: String) {
        _uiState.update { state ->
            val newSelection = if (state.selectedUserIds.contains(userId)) {
                state.selectedUserIds - userId
            } else {
                state.selectedUserIds + userId
            }
            state.copy(selectedUserIds = newSelection)
        }
    }

    private fun createGroup() {
        val state = _uiState.value
        if (!state.canCreate || state.isCreating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            val result = conversationRepository.createGroup(
                title = state.title,
                userIds = state.selectedUserIds.toList()
            )
            when (result) {
                is NetworkResult.Success -> {
                    _effect.send(CreateGroupEffect.NavigateToConversation(result.data.id))
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isCreating = false) }
                    _effect.send(CreateGroupEffect.ShowError(result.message))
                }
            }
        }
    }
}
