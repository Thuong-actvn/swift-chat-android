package com.thuo_ng.swift_chat_android.ui.chat

import android.net.Uri
import com.thuo_ng.swift_chat_android.domain.model.Conversation
import com.thuo_ng.swift_chat_android.domain.model.Message
import com.thuo_ng.swift_chat_android.domain.model.ReadReceipt
import com.thuo_ng.swift_chat_android.domain.model.TypingUser

data class ChatUiState(
    val conversationId: String = "",
    val conversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val readReceipts: List<ReadReceipt> = emptyList(),
    val typingUsers: List<TypingUser> = emptyList(),
    val currentAccountId: String? = null,
    val inputText: String = "",
    val isInputFocused: Boolean = false,
    val isInitialSyncing: Boolean = false,
    val isLoadingOlder: Boolean = false,
    val hasMoreOlderMessages: Boolean = true,
    val errorMessage: String? = null
) {
    val canSend: Boolean get() = inputText.isNotBlank()
    val shouldShowSendButton: Boolean get() = inputText.isNotBlank() || isInputFocused
}

sealed class ChatIntent {
    data class Start(val conversationId: String) : ChatIntent()
    data class InputChanged(val value: String) : ChatIntent()
    data class InputFocusChanged(val focused: Boolean) : ChatIntent()
    data object SendClicked : ChatIntent()
    data class AttachmentSelected(val uri: Uri, val type: String) : ChatIntent()
    data object LoadOlder : ChatIntent()
    data class RetryMessage(val message: Message) : ChatIntent()
    data class UnsendMessage(val message: Message) : ChatIntent()
    data class DeleteForMe(val message: Message) : ChatIntent()
    data class ToggleReaction(val message: Message, val emoji: String) : ChatIntent()
    data class TogglePin(val message: Message) : ChatIntent()
}

sealed class ChatEffect {
    data class ShowMessage(val message: String) : ChatEffect()
}
