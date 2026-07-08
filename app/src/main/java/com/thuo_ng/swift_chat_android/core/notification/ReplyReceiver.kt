package com.thuo_ng.swift_chat_android.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.thuo_ng.swift_chat_android.domain.repository.ChatRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReplyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var chatRepository: ChatRepository

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val conversationId = intent.getStringExtra("conversationId") ?: return
        val senderName = intent.getStringExtra("senderName") ?: "Unknown"

        if (remoteInput != null) {
            val replyText = remoteInput.getCharSequence(NotificationHelper.KEY_TEXT_REPLY)?.toString()
            if (!replyText.isNullOrBlank()) {
                // Show "Sending..." status
                updateNotification(context, conversationId, senderName, "Đang gửi...")

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Using sendTextMessage from ChatRepository
                        chatRepository.sendTextMessage(conversationId, replyText)
                        
                        // Show "Sent" status or dismiss
                        updateNotification(context, conversationId, senderName, "Đã gửi")
                        
                        // Alternatively, dismiss after a delay
                        // NotificationManagerCompat.from(context).cancel(conversationId.hashCode())
                    } catch (e: Exception) {
                        updateNotification(context, conversationId, senderName, "Gửi thất bại")
                    }
                }
            }
        }
    }

    private fun updateNotification(
        context: Context,
        conversationId: String,
        senderName: String,
        status: String
    ) {
        val notificationManager = NotificationManagerCompat.from(context)
        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentTitle(senderName)
            .setContentText(status)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)

        notificationManager.notify(conversationId.hashCode(), builder.build())
    }
}
