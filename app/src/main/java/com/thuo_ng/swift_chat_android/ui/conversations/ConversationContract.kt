package com.thuo_ng.swift_chat_android.ui.conversations

import com.thuo_ng.swift_chat_android.domain.model.Conversation

enum class ConversationFilter {
    Chats,
    Groups
}

data class ConversationUiState(
    val conversations: List<Conversation> = emptyList(),
    val selectedFilter: ConversationFilter = ConversationFilter.Chats,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = false
) {
    val visibleConversations: List<Conversation>
        get() = when (selectedFilter) {
            ConversationFilter.Chats -> conversations
            ConversationFilter.Groups -> conversations.filter { conversation ->
                conversation.type.equals("group", ignoreCase = true)
            }
        }
}

sealed class ConversationIntent {
    data object Refresh : ConversationIntent()
    data class FilterChanged(val filter: ConversationFilter) : ConversationIntent()
    data class ConversationClicked(val conversationId: String) : ConversationIntent()
}

sealed class ConversationEffect {
    data class NavigateToConversation(val conversationId: String) : ConversationEffect()
    data class ShowError(val message: String) : ConversationEffect()
}
