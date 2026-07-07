package com.thuo_ng.swift_chat_android.core.socket

data class SendMessagePayload(
    val conversationId: String,
    val content: String,
    val type: String = "text",        // "text" | "image" | "file"
    val clientTempId: String,         // UUID tạo phía client để match optimistic update
    val replyToId: String? = null
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
    val messageId: String             // messageId cuối cùng đã đọc
)

// Dùng chung cho các action chỉ cần messageId và conversationId (unsend, delete_for_me, pin, unpin)
data class MessageActionPayload(
    val messageId: String,
    val conversationId: String
)
