package com.thuo_ng.swift_chat_android.domain.repository

import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.domain.model.ConversationPage

interface ConversationRepository {
    suspend fun getConversations(): NetworkResult<ConversationPage>
}
