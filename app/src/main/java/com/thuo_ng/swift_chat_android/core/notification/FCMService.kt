package com.thuo_ng.swift_chat_android.core.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import com.thuo_ng.swift_chat_android.domain.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {

    @Inject
    lateinit var secureStorage: SecureStorage

    @Inject
    lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Suppress("DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "Refreshed token: $token")
        
        secureStorage.saveFcmToken(token)
        
        // If user is logged in, update token on server
        if (authRepository.isUserLoggedIn()) {
            serviceScope.launch {
                authRepository.registerDeviceToken(token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCMService", "From: ${remoteMessage.from}")

        // Handle Data Message
        if (remoteMessage.data.isNotEmpty()) {
            val senderName = remoteMessage.data["title"] ?: "New Message"
            val content = remoteMessage.data["body"] ?: ""
            val conversationId = remoteMessage.data["conversationId"] ?: ""
            val type = remoteMessage.data["type"]

            if (type == "NEW_MESSAGE" && conversationId.isNotEmpty()) {
                NotificationHelper(applicationContext).showChatNotification(
                    senderName = senderName,
                    messageContent = content,
                    conversationId = conversationId
                )
            }
        }
        
        // Notification message is handled automatically by the system when app is in background.
        // But since we are using Data Message, we handle it here.
    }
}
