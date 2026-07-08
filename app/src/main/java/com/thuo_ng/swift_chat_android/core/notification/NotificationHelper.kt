package com.thuo_ng.swift_chat_android.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.thuo_ng.swift_chat_android.MainActivity
import com.thuo_ng.swift_chat_android.R
import com.thuo_ng.swift_chat_android.core.util.AvatarGenerator

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "chat_notifications"
        const val KEY_TEXT_REPLY = "key_text_reply"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Chat Messages"
            val descriptionText = "Notifications for new chat messages"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showChatNotification(
        senderName: String,
        messageContent: String,
        conversationId: String
    ) {
        val avatarSize = 128
        val avatarBitmap = AvatarGenerator.generate(senderName, avatarSize)
        val senderIcon = IconCompat.createWithBitmap(avatarBitmap)

        val user = Person.Builder()
            .setName("Me") // In a real app, you'd get the current user's name
            .build()

        val sender = Person.Builder()
            .setName(senderName)
            .setIcon(senderIcon)
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(user)
            .addMessage(messageContent, System.currentTimeMillis(), sender)
            .setConversationTitle(senderName)

        // RemoteInput for Quick Reply
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Trả lời...")
            .build()

        val replyIntent = Intent(context, ReplyReceiver::class.java).apply {
            putExtra("conversationId", conversationId)
            putExtra("senderName", senderName)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            conversationId.hashCode(),
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Trả lời",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        // Intent to open the app when notification is clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("conversationId", conversationId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            conversationId.hashCode(), 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(replyAction)
            .setColor(ContextCompat.getColor(context, R.color.swift_primary))

        with(NotificationManagerCompat.from(context)) {
            notify(conversationId.hashCode(), builder.build())
        }
    }
}
