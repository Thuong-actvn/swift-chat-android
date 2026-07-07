package com.thuo_ng.swift_chat_android.ui.conversations.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thuo_ng.swift_chat_android.domain.model.Conversation
import com.thuo_ng.swift_chat_android.ui.components.UnreadBadge
import com.thuo_ng.swift_chat_android.ui.components.UserAvatar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            avatarUrl = conversation.displayInfo.avatarUrl,
            size = 56.dp,
            showOnlineDot = conversation.type.equals("direct", ignoreCase = true),
            isOnline = conversation.displayInfo.isOnline == true
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.displayInfo.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                val timestamp = conversation.lastMessage?.timestamp ?: conversation.updatedAt
                Text(
                    text = formatTimestamp(timestamp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (conversation.unreadCount > 0) Color(0xFF007A8A) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val lastMessageText = if (conversation.lastMessage != null) {
                    val prefix = if (
                        conversation.type.equals("group", ignoreCase = true) &&
                        conversation.lastMessage.senderName != null
                    ) {
                        "${conversation.lastMessage.senderName}: "
                    } else ""
                    prefix + conversation.lastMessage.content
                } else {
                    "No messages yet"
                }

                Text(
                    text = lastMessageText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unreadCount > 0) {
                    UnreadBadge(
                        count = conversation.unreadCount,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        val now = Instant.now().atZone(ZoneId.systemDefault())

        val daysBetween = ChronoUnit.DAYS.between(zonedDateTime.toLocalDate(), now.toLocalDate())

        when {
            daysBetween == 0L -> zonedDateTime.format(DateTimeFormatter.ofPattern("h:mm a"))
            daysBetween == 1L -> "Yesterday"
            daysBetween < 7L -> zonedDateTime.format(DateTimeFormatter.ofPattern("EEE"))
            else -> zonedDateTime.format(DateTimeFormatter.ofPattern("MMM dd"))
        }
    } catch (e: Exception) {
        ""
    }
}
