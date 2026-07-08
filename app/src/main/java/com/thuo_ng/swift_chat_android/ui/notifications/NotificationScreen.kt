package com.thuo_ng.swift_chat_android.ui.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.thuo_ng.swift_chat_android.domain.model.Notification
import com.thuo_ng.swift_chat_android.domain.model.isAddedToGroupType
import com.thuo_ng.swift_chat_android.domain.model.isFriendRequestAcceptedType
import com.thuo_ng.swift_chat_android.domain.model.isFriendRequestReceivedType
import com.thuo_ng.swift_chat_android.domain.model.isGroupRoleChangedType
import com.thuo_ng.swift_chat_android.domain.model.isNewMessageType
import com.thuo_ng.swift_chat_android.domain.model.isRemovedFromGroupType
import com.thuo_ng.swift_chat_android.domain.model.normalizeNotificationType
import com.thuo_ng.swift_chat_android.ui.theme.Amber90
import com.thuo_ng.swift_chat_android.ui.theme.Amber40
import com.thuo_ng.swift_chat_android.ui.theme.Blue95
import com.thuo_ng.swift_chat_android.ui.theme.Blue40
import com.thuo_ng.swift_chat_android.ui.theme.Green90
import com.thuo_ng.swift_chat_android.ui.theme.Green30
import com.thuo_ng.swift_chat_android.ui.theme.Neutral100
import com.thuo_ng.swift_chat_android.ui.theme.Neutral90
import com.thuo_ng.swift_chat_android.ui.theme.Red90
import com.thuo_ng.swift_chat_android.ui.theme.Red40
import com.thuo_ng.swift_chat_android.ui.theme.SwiftChatThemeTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NotificationScreen(
    onOpenConversation: (String) -> Unit = {},
    onNavigateToFriendsReceived: () -> Unit = {},
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NotificationEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                is NotificationEffect.OpenConversation -> onOpenConversation(effect.conversationId)
                NotificationEffect.OpenFriendsReceived -> onNavigateToFriendsReceived()
            }
        }
    }

    NotificationContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onRefresh = { viewModel.handleIntent(NotificationIntent.Refresh) },
        onMarkAllAsRead = { viewModel.handleIntent(NotificationIntent.MarkAllAsRead) },
        onNotificationClick = { viewModel.handleIntent(NotificationIntent.NotificationClicked(it)) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationContent(
    state: NotificationUiState,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onNotificationClick: (Notification) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    actions = {
                        TextButton(onClick = onMarkAllAsRead, enabled = state.unreadCount > 0) {
                            Text(text = "Mark all as read")
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                state.isLoading -> NotificationLoadingState()
                state.notifications.isEmpty() -> NotificationEmptyState(
                    title = "No notifications yet",
                    message = "You will see friend requests, group updates and new messages here."
                )
                else -> NotificationList(
                    sections = state.sections,
                    onNotificationClick = onNotificationClick
                )
            }
        }
    }
}

@Composable
private fun NotificationList(
    sections: List<NotificationSection>,
    onNotificationClick: (Notification) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = sections,
            key = { it.label }
        ) { section ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = section.label.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                )

                section.items.forEach { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = { onNotificationClick(notification) }
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visual = rememberNotificationVisual(notification)
    val relativeTime = formatRelativeTime(notification.createdAt)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            onClick = onClick
        ) {
            Column(modifier = Modifier.padding(horizontal = 0.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    NotificationAvatar(
                        notification = notification,
                        icon = visual.icon,
                        background = visual.background,
                        contentColor = visual.contentColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (notification.message.shouldShowAsSubtitle()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = notification.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = relativeTime,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationAvatar(
    notification: Notification,
    icon: ImageVector,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val avatarUrl = notification.actor?.avatarUrl

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = notification.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )
        }
    }
}

@Composable
private fun NotificationLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Loading notifications...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotificationEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class NotificationVisual(
    val icon: ImageVector,
    val background: Color,
    val contentColor: Color
)

@Composable
private fun rememberNotificationVisual(notification: Notification): NotificationVisual {
    return when {
        notification.type.isFriendRequestReceivedType() || notification.type.isFriendRequestAcceptedType() -> NotificationVisual(
            icon = Icons.Outlined.Person,
            background = Blue95,
            contentColor = Blue40
        )
        notification.type.isAddedToGroupType() -> NotificationVisual(
            icon = Icons.Outlined.Group,
            background = Green90,
            contentColor = Green30
        )
        notification.type.isRemovedFromGroupType() -> NotificationVisual(
            icon = Icons.Outlined.Group,
            background = Red90,
            contentColor = Red40
        )
        notification.type.isGroupRoleChangedType() -> NotificationVisual(
            icon = Icons.Outlined.Group,
            background = Amber90,
            contentColor = Amber40
        )
        notification.type.isNewMessageType() -> NotificationVisual(
            icon = Icons.AutoMirrored.Outlined.Chat,
            background = Blue95,
            contentColor = Blue40
        )
        else -> NotificationVisual(
            icon = Icons.Outlined.Notifications,
            background = Neutral90,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatRelativeTime(createdAt: String): String {
    val instant = runCatching { Instant.parse(createdAt) }
        .getOrElse {
            runCatching { java.time.OffsetDateTime.parse(createdAt).toInstant() }
                .getOrElse { Instant.EPOCH }
        }
    val now = Instant.now()
    val minutes = java.time.Duration.between(instant, now).toMinutes()
    val hours = java.time.Duration.between(instant, now).toHours()

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> {
            val zoneId = ZoneId.systemDefault()
            instant.atZone(zoneId).format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH))
        }
    }
}

private fun String.shouldShowAsSubtitle(): Boolean {
    val normalized = normalizeNotificationType()
    if (this.isBlank()) return false
    if (this == "Open to view details.") return false
    return when (normalized) {
        "friend_request_received",
        "friend_request_accepted",
        "added_to_group",
        "removed_from_group",
        "group_role_changed",
        "new_message" -> true
        else -> false
    }
}