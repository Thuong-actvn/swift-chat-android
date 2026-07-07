package com.thuo_ng.swift_chat_android.core.socket

data class SendMessagePayload(
    val conversationId: String,
    val content: String? = null,
    val type: String = "text",
    val clientTempId: String,
    val replyToMessageId: String? = null,
    val attachments: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    val forwardFromMessageId: String? = null
)

data class SocketAckResult(
    val status: String? = null,
    val messageId: String? = null,
    val error: String? = null
)

data class EditMessagePayload(
    val messageId: String,
    val conversationId: String,
    val content: String
)

data class ReactPayload(
    val messageId: String,
    val conversationId: String,
    val emoji: String
)

data class MarkReadPayload(
    val conversationId: String,
    val messageId: String
)

data class MessageActionPayload(
    val messageId: String,
    val conversationId: String
)
