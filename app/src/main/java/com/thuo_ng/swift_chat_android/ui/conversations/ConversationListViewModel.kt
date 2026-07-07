package com.thuo_ng.swift_chat_android.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import com.thuo_ng.swift_chat_android.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
class ConversationListViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ConversationUiState(currentAccountId = secureStorage.getUserId())
    )
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ConversationEffect>(Channel.BUFFERED)
    val effect: Flow<ConversationEffect> = _effect.receiveAsFlow()

    init {
        observeConversations()
        syncConversations(isRefresh = false)
    }

    fun handleIntent(intent: ConversationIntent) {
        when (intent) {
            is ConversationIntent.Refresh -> syncConversations(isRefresh = true)
            is ConversationIntent.FilterChanged -> {
                _uiState.update { it.copy(selectedFilter = intent.filter) }
            }
            is ConversationIntent.ConversationClicked -> {
                viewModelScope.launch {
                    _effect.send(ConversationEffect.NavigateToConversation(intent.conversationId))
                }
            }
        }
    }

    private fun observeConversations() {
        viewModelScope.launch {
            conversationRepository.observeConversations().collect { conversations ->
                _uiState.update {
                    it.copy(
                        conversations = conversations,
                        isLoading = if (conversations.isNotEmpty()) false else it.isLoading
                    )
                }
            }
        }
    }

    private fun syncConversations(isRefresh: Boolean) {
        if (isRefresh && (_uiState.value.isLoading || _uiState.value.isRefreshing)) return

        viewModelScope.launch {
            val shouldShowInitialLoading = !isRefresh && _uiState.value.conversations.isEmpty()

            _uiState.update {
                it.copy(
                    isLoading = shouldShowInitialLoading,
                    isRefreshing = isRefresh,
                    errorMessage = null
                )
            }

            when (val result = conversationRepository.syncConversations()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            nextCursor = result.data.nextCursor,
                            hasMore = result.data.hasMore,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = result.message
                        )
                    }
                    _effect.send(ConversationEffect.ShowError(result.message))
                }
            }
        }
    }
}
