package com.thuo_ng.swift_chat_android.ui.creategroup

import com.thuo_ng.swift_chat_android.domain.model.PublicUserProfile

data class CreateGroupUiState(
    val title: String = "",
    val friends: List<PublicUserProfile> = emptyList(),
    val selectedUserIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val searchQuery: String = "",
    val errorMessage: String? = null
) {
    val canCreate: Boolean get() = title.isNotBlank() && selectedUserIds.isNotEmpty()
    
    val filteredFriends: List<PublicUserProfile> get() = if (searchQuery.isBlank()) {
        friends
    } else {
        friends.filter { 
            it.displayName?.contains(searchQuery, ignoreCase = true) == true ||
            it.handle.contains(searchQuery, ignoreCase = true)
        }
    }
}

sealed interface CreateGroupIntent {
    data class TitleChanged(val title: String) : CreateGroupIntent
    data class SearchQueryChanged(val query: String) : CreateGroupIntent
    data class UserSelected(val userId: String) : CreateGroupIntent
    data object CreateClicked : CreateGroupIntent
    data object Refresh : CreateGroupIntent
}

sealed interface CreateGroupEffect {
    data class NavigateToConversation(val conversationId: String) : CreateGroupEffect
    data class ShowError(val message: String) : CreateGroupEffect
}
