package com.thuo_ng.swift_chat_android.ui.chat

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.thuo_ng.swift_chat_android.core.util.rememberImagePicker
import com.thuo_ng.swift_chat_android.domain.model.Conversation
import com.thuo_ng.swift_chat_android.domain.model.CurrentParticipant
import com.thuo_ng.swift_chat_android.domain.model.DisplayInfo
import com.thuo_ng.swift_chat_android.domain.model.Message
import com.thuo_ng.swift_chat_android.domain.model.MessageAttachment
import com.thuo_ng.swift_chat_android.domain.model.MessageReaction
import com.thuo_ng.swift_chat_android.domain.model.ParticipantPreview
import com.thuo_ng.swift_chat_android.domain.model.ReadReceipt
import com.thuo_ng.swift_chat_android.domain.model.SendStatus
import com.thuo_ng.swift_chat_android.domain.model.SenderProfile
import com.thuo_ng.swift_chat_android.ui.components.FullScreenImageDialog
import com.thuo_ng.swift_chat_android.ui.components.UserAvatar
import com.thuo_ng.swift_chat_android.ui.theme.Blue40
import com.thuo_ng.swift_chat_android.ui.theme.LocalChatColor
import com.thuo_ng.swift_chat_android.ui.theme.Neutral100
import com.thuo_ng.swift_chat_android.ui.theme.NeutralVariant50
import com.thuo_ng.swift_chat_android.ui.theme.SwiftChatTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ChatDetailScreen(
    conversationId: String? = null,
    pendingPartnerId: String? = null,
    pendingDisplayName: String? = null,
    pendingAvatarUrl: String? = null,
    onBack: () -> Unit,
    onOpenConversationInfo: (String) -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(conversationId, pendingPartnerId) {
        if (conversationId != null) {
            viewModel.handleIntent(ChatIntent.Start(conversationId))
        } else if (pendingPartnerId != null && pendingDisplayName != null) {
            viewModel.handleIntent(
                ChatIntent.StartPendingDirect(
                    partnerId = pendingPartnerId,
                    displayName = pendingDisplayName,
                    avatarUrl = pendingAvatarUrl
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChatEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ChatDetailContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onOpenConversationInfo = onOpenConversationInfo,
        onNavigateToUserProfile = onNavigateToUserProfile,
        onInputChanged = { viewModel.handleIntent(ChatIntent.InputChanged(it)) },
        onInputFocusChanged = { viewModel.handleIntent(ChatIntent.InputFocusChanged(it)) },
        onSendClick = { viewModel.handleIntent(ChatIntent.SendClicked) },
        onAttachmentSelected = { uri, type ->
            viewModel.handleIntent(ChatIntent.AttachmentSelected(uri, type))
        },
        onLoadOlder = { viewModel.handleIntent(ChatIntent.LoadOlder) },
        onRetryMessage = { viewModel.handleIntent(ChatIntent.RetryMessage(it)) },
        onUnsendMessage = { viewModel.handleIntent(ChatIntent.UnsendMessage(it)) },
        onDeleteForMe = { viewModel.handleIntent(ChatIntent.DeleteForMe(it)) },
        onReact = { message, emoji -> viewModel.handleIntent(ChatIntent.ToggleReaction(message, emoji)) },
        onTogglePin = { viewModel.handleIntent(ChatIntent.TogglePin(it)) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatDetailContent(
    state: ChatUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenConversationInfo: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onInputChanged: (String) -> Unit,
    onInputFocusChanged: (Boolean) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentSelected: (Uri, String) -> Unit,
    onLoadOlder: () -> Unit,
    onRetryMessage: (Message) -> Unit,
    onUnsendMessage: (Message) -> Unit,
    onDeleteForMe: (Message) -> Unit,
    onReact: (Message, String) -> Unit,
    onTogglePin: (Message) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val rows = remember(state.messages) { buildChatRows(state.messages) }
    var paginationEnabled by remember(state.conversationId) { mutableStateOf(false) }

    LaunchedEffect(rows.lastOrNull()?.stableKey) {
        if (rows.isNotEmpty()) {
            listState.animateScrollToItem(rows.lastIndex)
            paginationEnabled = true
        }
    }

    LaunchedEffect(state.isInputFocused) {
        if (state.isInputFocused && rows.isNotEmpty()) {
            listState.animateScrollToItem(rows.lastIndex)
        }
    }

    LaunchedEffect(
        listState,
        paginationEnabled,
        state.isInitialSyncing,
        state.hasMoreOlderMessages
    ) {
        if (!paginationEnabled || state.isInitialSyncing || !state.hasMoreOlderMessages) return@LaunchedEffect

        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (index <= 1 && !state.isLoadingOlder) {
                    onLoadOlder()
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
    ) {
        ChatTopBar(
            state = state,
            onBack = onBack,
            onOpenConversationInfo = onOpenConversationInfo,
            onNavigateToUserProfile = onNavigateToUserProfile
        )

        Box(modifier = Modifier.weight(1f)) {
            MessageList(
                rows = rows,
                state = state,
                listState = listState,
                onRetryMessage = onRetryMessage,
                onUnsendMessage = onUnsendMessage,
                onDeleteForMe = onDeleteForMe,
                onReact = onReact,
                onTogglePin = onTogglePin,
                onNavigateToUserProfile = onNavigateToUserProfile,
                modifier = Modifier.fillMaxWidth(),
                bottomContentPadding = 12.dp
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }

        ChatInputBar(
            text = state.inputText,
            showSend = state.shouldShowSendButton,
            canSend = state.canSend,
            onTextChange = onInputChanged,
            onFocusChanged = onInputFocusChanged,
            onSendClick = onSendClick,
            onAttachmentSelected = onAttachmentSelected
        )
    }
}

@Composable
private fun ChatTopBar(
    state: ChatUiState,
    onBack: () -> Unit,
    onOpenConversationInfo: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit
) {
    val conversation = state.conversation
    val partnerId = state.pendingDirect?.partnerId
        ?: conversation?.participantPreview?.find { it.accountId != state.currentAccountId }?.accountId

    val typingText = state.typingUsers.firstOrNull()?.displayName?.let { "$it is typing..." }
        ?: state.typingUsers.firstOrNull()?.let { "typing..." }
    val subtitle = typingText ?: when {
        conversation?.displayInfo?.isOnline == true -> "Active now"
        conversation?.type.equals("group", ignoreCase = true) -> "${conversation?.totalParticipants ?: 0} members"
        else -> "Offline"
    }

    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                UserAvatar(
                    avatarUrl = conversation?.displayInfo?.avatarUrl,
                    size = 40.dp,
                    showOnlineDot = conversation?.type.equals("direct", ignoreCase = true),
                    isOnline = conversation?.displayInfo?.isOnline == true,
                    modifier = Modifier.clickable(
                        enabled = partnerId != null && !conversation?.type.equals("group", true),
                        onClick = { partnerId?.let(onNavigateToUserProfile) }
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            enabled = partnerId != null && !conversation?.type.equals("group", true),
                            onClick = { partnerId?.let(onNavigateToUserProfile) }
                        )
                ) {
                    Text(
                        text = conversation?.displayInfo?.title ?: "Conversation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (conversation?.displayInfo?.isOnline == true || typingText != null) {
                            Color(0xFF007A22)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = {}, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Outlined.Videocam, contentDescription = "Video call", modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = {}, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Outlined.Call, contentDescription = "Voice call", modifier = Modifier.size(22.dp))
                }
                IconButton(
                    onClick = {
                        state.conversationId.takeIf { it.isNotBlank() }?.let(onOpenConversationInfo)
                    },
                    enabled = state.conversationId.isNotBlank(),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", modifier = Modifier.size(22.dp))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        }
    }
}

@Composable
private fun MessageList(
    rows: List<ChatListRow>,
    state: ChatUiState,
    listState: LazyListState,
    onRetryMessage: (Message) -> Unit,
    onUnsendMessage: (Message) -> Unit,
    onDeleteForMe: (Message) -> Unit,
    onReact: (Message, String) -> Unit,
    onTogglePin: (Message) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    bottomContentPadding: androidx.compose.ui.unit.Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 8.dp,
                top = 28.dp,
                end = 8.dp,
                bottom = bottomContentPadding
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.isLoadingOlder) {
                item(key = "loading-older") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    }
                }
            }

            items(
                items = rows,
                key = { it.stableKey }
            ) { row ->
                when (row) {
                    is ChatListRow.DaySeparator -> DayChip(row.label)
                    is ChatListRow.MessageItem -> {
                        val isOwn = row.message.senderId == state.currentAccountId
                        val isGroupConversation = state.conversation?.type.equals("group", true)
                        val senderInfo = if (!isOwn) resolveSenderInfo(row.message, state) else null
                        
                        MessageBubble(
                            message = row.message,
                            isOwn = isOwn,
                            showTime = row.showTime,
                            showName = row.showName && isGroupConversation,
                            showAvatar = !isOwn && (isGroupConversation || row.showTime),
                            senderName = senderInfo?.displayName,
                            senderAvatar = senderInfo?.avatarUrl,
                            isRead = isMessageRead(row.message, state.readReceipts),
                            onRetry = { onRetryMessage(row.message) },
                            onUnsend = { onUnsendMessage(row.message) },
                            onDeleteForMe = { onDeleteForMe(row.message) },
                            onReact = { emoji -> onReact(row.message, emoji) },
                            onTogglePin = { onTogglePin(row.message) },
                            onAvatarClick = { onNavigateToUserProfile(row.message.senderId) }
                        )
                    }
                }
            }
        }

        if (state.isInitialSyncing && state.messages.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp),
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
private fun DayChip(label: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFE9E4E6))
                .padding(horizontal = 16.dp, vertical = 5.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isOwn: Boolean,
    showTime: Boolean,
    showName: Boolean,
    showAvatar: Boolean,
    senderName: String?,
    senderAvatar: String?,
    isRead: Boolean,
    onRetry: () -> Unit,
    onUnsend: () -> Unit,
    onDeleteForMe: () -> Unit,
    onReact: (String) -> Unit,
    onTogglePin: () -> Unit,
    onAvatarClick: () -> Unit = {}
) {
    val colors = LocalChatColor.current
    val bubbleColor = if (isOwn) Color(0xFF0076A2) else colors.bubbleReceived
    val contentColor = if (isOwn) Neutral100 else colors.onBubbleReceived
    val bubbleShape = RoundedCornerShape(20.dp)
    var showActions by remember(message.localId) { mutableStateOf(false) }
    var fullScreenImageUrl by remember(message.localId) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val firstAttachment = message.attachments.minByOrNull { it.position }
    val isImageMessage = message.type.equals("image", ignoreCase = true)
    val imagePreviewUrl = firstAttachment?.url?.takeIf { isImageMessage && it.isNotBlank() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        if (showName && senderName != null && !isOwn) {
            Text(
                text = senderName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 40.dp, bottom = 4.dp)
                    .clickable(onClick = onAvatarClick)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isOwn) {
                if (showAvatar) {
                    UserAvatar(
                        avatarUrl = senderAvatar,
                        size = 32.dp,
                        modifier = Modifier.clickable(onClick = onAvatarClick)
                    )
                } else {
                    Spacer(modifier = Modifier.width(28.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Box(
                modifier = Modifier
                    .widthIn(min = 40.dp, max = 270.dp)
            ) {
                val hasAttachments = message.attachments.isNotEmpty()
                Column(
                    modifier = Modifier
                        .align(if (isOwn) Alignment.CenterEnd else Alignment.CenterStart)
                        .clip(bubbleShape)
                        .then(if (hasAttachments) Modifier else Modifier.background(bubbleColor))
                        .combinedClickable(
                            onClick = {
                                imagePreviewUrl?.let { fullScreenImageUrl = it }
                            },
                            onLongClick = { showActions = true }
                        )
                        .padding(
                            horizontal = if (hasAttachments) 0.dp else 16.dp,
                            vertical = if (hasAttachments) 0.dp else 12.dp
                        )
                ) {
                    if (message.isUnsent) {
                        Text(
                            text = "Message unsent",
                            color = contentColor.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        MessageAttachmentContent(
                            message = message,
                            isOwn = isOwn,
                            onImageClick = { attachment -> fullScreenImageUrl = attachment.url },
                            onAttachmentLongClick = { showActions = true },
                            onDownloadClick = { attachment -> startAttachmentDownload(context, attachment.url) }
                        )
                        if (message.content.isNotBlank()) {
                            Text(
                                text = message.content,
                                color = if (hasAttachments) MaterialTheme.colorScheme.onSurface else contentColor,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp,
                                    letterSpacing = 0.sp
                                ),
                                modifier = if (hasAttachments) Modifier.padding(horizontal = 8.dp, vertical = 4.dp) else Modifier
                            )
                        }
                    }
                }

                if (message.reactions.isNotEmpty()) {
                    ReactionCluster(
                        reactions = message.reactions,
                        modifier = Modifier
                            .align(if (isOwn) Alignment.BottomStart else Alignment.BottomEnd)
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) {
                                    val x = if (isOwn) -placeable.width / 2 else placeable.width / 2
                                    val y = placeable.height / 2
                                    placeable.placeRelative(x, y)
                                }
                            }
                    )
                }

                MessageActionsMenu(
                    expanded = showActions,
                    onDismissRequest = { showActions = false },
                    showRetry = message.sendStatus == SendStatus.FAILED,
                    showServerActions = message.serverId != null && !message.isUnsent,
                    showUnsend = isOwn && message.serverId != null && !message.isUnsent,
                    showDownload = firstAttachment != null && !message.isUnsent,
                    isPinned = message.isPinned,
                    onRetry = onRetry,
                    onDownload = { firstAttachment?.let { startAttachmentDownload(context, it.url) } },
                    onReact = onReact,
                    onTogglePin = onTogglePin,
                    onUnsend = onUnsend,
                    onDeleteForMe = onDeleteForMe
                )
            }
        }

        if (showTime) {
            MessageMeta(
                message = message,
                isOwn = isOwn,
                isRead = isRead,
                onRetry = onRetry,
                modifier = Modifier.padding(start = if (isOwn) 0.dp else 40.dp)
            )
        }
    }

    fullScreenImageUrl?.let { imageUrl ->
        FullScreenImageDialog(
            model = imageUrl,
            onDismissRequest = { fullScreenImageUrl = null },
            onDownloadClick = { startAttachmentDownload(context, imageUrl) }
        )
    }
}

@Composable
private fun MessageActionsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    showRetry: Boolean,
    showServerActions: Boolean,
    showUnsend: Boolean,
    showDownload: Boolean,
    isPinned: Boolean,
    onRetry: () -> Unit,
    onDownload: () -> Unit,
    onReact: (String) -> Unit,
    onTogglePin: () -> Unit,
    onUnsend: () -> Unit,
    onDeleteForMe: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.width(292.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showServerActions) {
                Text(
                    text = "React",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
                MESSAGE_REACTIONS.chunked(4).forEach { reactions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        reactions.forEach { emoji ->
                            ReactionPickerButton(
                                emoji = emoji,
                                onClick = {
                                    onDismissRequest()
                                    onReact(emoji)
                                }
                            )
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
            }

            if (showRetry) {
                MessageActionRow(
                    icon = Icons.Outlined.Refresh,
                    label = "Retry",
                    onClick = {
                        onDismissRequest()
                        onRetry()
                    }
                )
            }
            if (showDownload) {
                MessageActionRow(
                    icon = Icons.Outlined.Download,
                    label = "Download",
                    onClick = {
                        onDismissRequest()
                        onDownload()
                    }
                )
            }
            if (showServerActions) {
                MessageActionRow(
                    icon = Icons.Outlined.PushPin,
                    label = if (isPinned) "Unpin" else "Pin",
                    onClick = {
                        onDismissRequest()
                        onTogglePin()
                    }
                )
                if (showUnsend) {
                    MessageActionRow(
                        icon = Icons.Outlined.RemoveCircleOutline,
                        label = "Unsend",
                        onClick = {
                            onDismissRequest()
                            onUnsend()
                        }
                    )
                }
            }
            MessageActionRow(
                icon = Icons.Outlined.Delete,
                label = "Delete for me",
                contentColor = MaterialTheme.colorScheme.error,
                onClick = {
                    onDismissRequest()
                    onDeleteForMe()
                }
            )
        }
    }
}

@Composable
private fun ReactionPickerButton(
    emoji: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 22.sp,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun MessageActionRow(
    icon: ImageVector,
    label: String,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(contentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MessageAttachmentContent(
    message: Message,
    isOwn: Boolean,
    onImageClick: (MessageAttachment) -> Unit,
    onAttachmentLongClick: () -> Unit,
    onDownloadClick: (MessageAttachment) -> Unit
) {
    if (message.attachments.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        message.attachments.sortedBy { it.position }.forEach { attachment ->
            when (message.type.lowercase()) {
                "image" -> AsyncImage(
                    model = attachment.url,
                    contentDescription = "Image attachment",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp, max = 230.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .combinedClickable(
                            onClick = { onImageClick(attachment) },
                            onLongClick = onAttachmentLongClick
                        )
                )
                else -> FileAttachmentRow(
                    attachment = attachment,
                    isOwn = isOwn,
                    onDownloadClick = { onDownloadClick(attachment) }
                )
            }
        }
        if (message.content.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun FileAttachmentRow(
    attachment: MessageAttachment,
    isOwn: Boolean,
    onDownloadClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isOwn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
            contentDescription = null,
            tint = if (isOwn) Neutral100 else Blue40
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = attachment.url.substringAfterLast('/').ifBlank { "File attachment" },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isOwn) Neutral100 else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onDownloadClick,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = "Download attachment",
                tint = if (isOwn) Neutral100 else Blue40,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ReactionCluster(
    reactions: List<MessageReaction>,
    modifier: Modifier = Modifier
) {
    val summaries = remember(reactions) {
        reactions
            .mapNotNull { reaction -> reaction.emoji.takeIf { it.isNotBlank() } }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
    }
    val visibleSummaries = summaries.take(3)
    val hiddenCount = summaries.drop(3).sumOf { it.second }

    if (visibleSummaries.isEmpty()) return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            visibleSummaries.forEach { (emoji, count) ->
                Text(text = emoji, fontSize = 12.sp, lineHeight = 13.sp)
                if (count > 1) {
                    Text(
                        text = count.toString(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (hiddenCount > 0) {
                Text(
                    text = "+$hiddenCount",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MessageMeta(
    message: Message,
    isOwn: Boolean,
    isRead: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(top = 6.dp, start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = formatTime(message.createdAt),
            style = MaterialTheme.typography.labelMedium,
            color = NeutralVariant50
        )
        if (isOwn) {
            when (message.sendStatus) {
                SendStatus.FAILED -> Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = "Retry",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onRetry)
                )
                SendStatus.SENDING -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                SendStatus.SENT -> Icon(
                    imageVector = Icons.Outlined.DoneAll,
                    contentDescription = if (isRead) "Read" else "Sent",
                    tint = if (isRead) Color(0xFF0076A2) else NeutralVariant50,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    showSend: Boolean,
    canSend: Boolean,
    onTextChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentSelected: (Uri, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAttachMenu by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val imagePicker = rememberImagePicker { uri -> onAttachmentSelected(uri, "image") }
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                onAttachmentSelected(it, "file")
            }
        }
    )

    Surface(
        color = Color(0xFFF9F9F9),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    IconButton(
                        onClick = {
                            showEmojiPicker = false
                            showAttachMenu = true
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddCircleOutline,
                            contentDescription = "Attach",
                            modifier = Modifier.size(26.dp),
                            tint = Color(0xFF424242)
                        )
                    }
                    DropdownMenu(
                        expanded = showAttachMenu,
                        onDismissRequest = { showAttachMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Image") },
                            leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                            onClick = {
                                showAttachMenu = false
                                imagePicker.pickFromGallery()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("File") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, contentDescription = null) },
                            onClick = {
                                showAttachMenu = false
                                fileLauncher.launch(arrayOf("*/*"))
                            }
                        )
                    }
                }

                IconButton(onClick = imagePicker.takePhoto, modifier = Modifier.size(42.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = "Camera",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF424242)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                        TextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = {
                        Text(
                            text = "Message...",
                            color = Color(0xFF8E8E8E),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    trailingIcon = {
                        Box {
                            IconButton(
                                onClick = {
                                    showAttachMenu = false
                                    showEmojiPicker = !showEmojiPicker
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EmojiEmotions,
                                    contentDescription = "Emoji",
                                    modifier = Modifier.size(24.dp),
                                    tint = if (showEmojiPicker) Color(0xFF00668B) else Color(0xFF616161)
                                )
                            }
                            EmojiPickerMenu(
                                expanded = showEmojiPicker,
                                onDismissRequest = { showEmojiPicker = false },
                                onEmojiSelected = { emoji ->
                                    onTextChange(text + emoji)
                                }
                            )
                        }
                    },
                    singleLine = false,
                    maxLines = 4,
                    shape = RoundedCornerShape(25.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF0F0F0),
                        unfocusedContainerColor = Color(0xFFF0F0F0),
                        disabledContainerColor = Color(0xFFF0F0F0),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp,
                        letterSpacing = 0.sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(25.dp))
                        .onFocusChanged { onFocusChanged(it.isFocused) }
                )

                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00668B))
                        .clickable(enabled = canSend, onClick = onSendClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (showSend) Icons.AutoMirrored.Outlined.Send else Icons.Outlined.Mic,
                        contentDescription = if (showSend) "Send" else "Voice",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
    }
}

@Composable
private fun EmojiPickerMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onEmojiSelected: (String) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.width(292.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Emoji",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
            CHAT_INPUT_EMOJIS.chunked(5).forEach { rowEmojis ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowEmojis.forEach { emoji ->
                        ReactionPickerButton(
                            emoji = emoji,
                            onClick = { onEmojiSelected(emoji) }
                        )
                    }
                }
            }
        }
    }
}

private sealed class ChatListRow(val stableKey: String) {
    data class DaySeparator(val label: String, val dateKey: String) : ChatListRow("day-$dateKey")
    data class MessageItem(
        val message: Message,
        val showTime: Boolean,
        val showName: Boolean
    ) : ChatListRow("message-${message.localId}")
}

private fun buildChatRows(messages: List<Message>): List<ChatListRow> {
    val rows = mutableListOf<ChatListRow>()
    messages.forEachIndexed { index, message ->
        val date = parseInstant(message.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val previous = messages.getOrNull(index - 1)
        val previousDate = previous?.let {
            parseInstant(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        if (previousDate != date) {
            rows += ChatListRow.DaySeparator(label = formatDayChip(date), dateKey = date.toString())
        }

        val next = messages.getOrNull(index + 1)
        val isLastInSequence = next == null ||
            next.senderId != message.senderId ||
            isDifferentDay(message.createdAt, next.createdAt) ||
            Duration.between(parseInstant(message.createdAt), parseInstant(next.createdAt)).toMinutes() >= 5
        
        val showName = previous == null || 
            previous.senderId != message.senderId || 
            previousDate != date

        rows += ChatListRow.MessageItem(
            message = message, 
            showTime = isLastInSequence, 
            showName = showName
        )
    }
    return rows
}

private data class SenderUiInfo(
    val displayName: String?,
    val avatarUrl: String?
)

private fun resolveSenderInfo(message: Message, state: ChatUiState): SenderUiInfo {
    val senderProfile = message.sender
    val conversation = state.conversation

    if (conversation?.type.equals("direct", true) && message.senderId != state.currentAccountId) {
        return SenderUiInfo(
            displayName = firstNonBlank(
                conversation?.displayInfo?.title,
                senderProfile?.displayName,
                senderProfile?.handle,
                message.senderId
            ),
            avatarUrl = firstNonBlank(conversation?.displayInfo?.avatarUrl, senderProfile?.avatarUrl)
        )
    }

    val participant = conversation
        ?.participantPreview
        ?.firstOrNull { it.matchesSender(message.senderId, senderProfile) }
    val receipt = state.readReceipts.firstOrNull {
        it.hasProfileInfo() && it.matchesSender(message.senderId, senderProfile)
    }

    return SenderUiInfo(
        displayName = firstNonBlank(
            participant?.displayName,
            senderProfile?.displayName,
            receipt?.displayName,
            participant?.handle,
            senderProfile?.handle,
            receipt?.handle,
            message.senderId
        ),
        avatarUrl = firstNonBlank(
            participant?.avatarUrl,
            senderProfile?.avatarUrl,
            receipt?.avatarUrl
        )
    )
}

private fun ParticipantPreview.matchesSender(senderId: String, senderProfile: SenderProfile?): Boolean {
    val targets = senderTargets(senderId, senderProfile)
    return targets.any { target ->
        accountId.matchesId(target) || handle.matchesHandle(target)
    }
}

private fun ReadReceipt.matchesSender(senderId: String, senderProfile: SenderProfile?): Boolean {
    val targets = senderTargets(senderId, senderProfile)
    return targets.any { target ->
        accountId.matchesId(target) || handle.matchesHandle(target)
    }
}

private fun ReadReceipt.hasProfileInfo(): Boolean =
    !handle.isNullOrBlank() || !displayName.isNullOrBlank() || !avatarUrl.isNullOrBlank()

private fun senderTargets(senderId: String, senderProfile: SenderProfile?): List<String> =
    listOfNotNull(
        senderId.takeIfNotBlank(),
        senderProfile?.accountId.takeIfNotBlank(),
        senderProfile?.handle.takeIfNotBlank()
    ).distinct()

private fun String?.matchesId(value: String): Boolean =
    !isNullOrBlank() && this == value

private fun String?.matchesHandle(value: String): Boolean =
    !isNullOrBlank() && equals(value, ignoreCase = true)

private fun firstNonBlank(vararg values: String?): String? =
    values.firstOrNull { !it.isNullOrBlank() }

private fun String?.takeIfNotBlank(): String? =
    takeIf { !it.isNullOrBlank() }

private fun findSenderInfo(senderId: String, state: ChatUiState): com.thuo_ng.swift_chat_android.domain.model.ParticipantPreview? {
    val conversation = state.conversation ?: return null
    
    // 1. Nếu là chat direct và không phải mình gửi, dùng thông tin conversation (đối phương)
    if (conversation.type.equals("direct", true) && senderId != state.currentAccountId) {
        return com.thuo_ng.swift_chat_android.domain.model.ParticipantPreview(
            handle = senderId,
            displayName = conversation.displayInfo.title,
            avatarUrl = conversation.displayInfo.avatarUrl
        )
    }
    
    // 2. Ưu tiên tìm trong readReceipts vì thường chứa thông tin đầy đủ (ID, handle, displayName, avatar)
    val receipt = state.readReceipts.find { it.accountId == senderId || it.handle == senderId }
    if (receipt != null) {
        return com.thuo_ng.swift_chat_android.domain.model.ParticipantPreview(
            handle = receipt.handle ?: senderId,
            displayName = receipt.displayName ?: "User",
            avatarUrl = receipt.avatarUrl
        )
    }

    // 3. Tìm trong participantPreview của conversation
    val preview = conversation.participantPreview.find { it.handle == senderId }
    if (preview != null) return preview
    
    return null
}

private fun isMessageRead(message: Message, readReceipts: List<ReadReceipt>): Boolean {
    val serverId = message.serverId ?: return false
    return readReceipts.any { it.lastReadMessageId == serverId && it.accountId != message.senderId }
}

private fun isDifferentDay(first: String, second: String): Boolean {
    val zone = ZoneId.systemDefault()
    return parseInstant(first).atZone(zone).toLocalDate() != parseInstant(second).atZone(zone).toLocalDate()
}

private fun formatDayChip(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
    }
}

private fun formatTime(value: String): String {
    return parseInstant(value)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
}

private fun parseInstant(value: String): Instant {
    return runCatching { Instant.parse(value) }
        .getOrElse {
            runCatching { java.time.OffsetDateTime.parse(value).toInstant() }
                .getOrElse { Instant.EPOCH }
        }
}

private fun startAttachmentDownload(context: Context, url: String) {
    if (!URLUtil.isNetworkUrl(url)) {
        Toast.makeText(context, "Cannot download this attachment", Toast.LENGTH_SHORT).show()
        return
    }

    val fileName = URLUtil.guessFileName(url, null, null).ifBlank { "swift-chat-attachment" }
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle(fileName)
        .setDescription("Swift Chat attachment")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

    val downloadManager = context.getSystemService(DownloadManager::class.java)
    runCatching { downloadManager.enqueue(request) }
        .onSuccess {
            Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
        }
        .onFailure {
            Toast.makeText(context, "Could not start download", Toast.LENGTH_SHORT).show()
        }
}

private val MESSAGE_REACTIONS = listOf(
    "\uD83D\uDC4D",
    "\u2764\uFE0F",
    "\uD83D\uDE02",
    "\uD83D\uDE2E",
    "\uD83D\uDE22",
    "\uD83D\uDD25",
    "\uD83C\uDF89",
    "\uD83D\uDE4F"
)

private val CHAT_INPUT_EMOJIS = listOf(
    "\uD83D\uDE00",
    "\uD83D\uDE01",
    "\uD83D\uDE02",
    "\uD83E\uDD23",
    "\uD83D\uDE0D",
    "\uD83E\uDD70",
    "\uD83D\uDE18",
    "\uD83D\uDE0E",
    "\uD83E\uDD29",
    "\uD83E\uDD14",
    "\uD83D\uDE22",
    "\uD83D\uDE2D",
    "\uD83D\uDE21",
    "\uD83D\uDE31",
    "\uD83D\uDE34",
    "\uD83D\uDC4D",
    "\uD83D\uDC4F",
    "\uD83D\uDE4F",
    "\u2764\uFE0F",
    "\uD83D\uDD25",
    "\uD83C\uDF89",
    "\u2728",
    "\uD83D\uDCAF",
    "\uD83D\uDC40",
    "\uD83E\uDD1D"
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChatDetailPreview() {
    SwiftChatTheme {
        ChatDetailContent(
            state = ChatUiState(
                conversation = Conversation(
                    id = "1",
                    type = "direct",
                    displayInfo = DisplayInfo("Alex Rivera", null, true),
                    createdAt = Instant.now().toString(),
                    updatedAt = Instant.now().toString(),
                    unreadCount = 0,
                    currentParticipant = CurrentParticipant("member", false, null, null),
                    participantPreview = emptyList(),
                    totalParticipants = 2,
                    lastMessage = null
                ),
                messages = listOf(
                    previewMessage("m1", "user-2", "Hey! Are we still on for the design sync later today?", "2026-07-06T02:41:00Z"),
                    previewMessage("m2", "me", "Yes, definitely! 3 PM works best for me.", "2026-07-06T02:45:00Z"),
                    previewMessage("m3", "user-2", "Perfect. I'll send over the Figma link in a bit.", "2026-07-07T03:02:00Z")
                ),
                currentAccountId = "me",
                inputText = ""
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onOpenConversationInfo = {},
            onInputChanged = {},
            onInputFocusChanged = {},
            onSendClick = {},
            onAttachmentSelected = { _, _ -> },
            onLoadOlder = {},
            onRetryMessage = {},
            onUnsendMessage = {},
            onDeleteForMe = {},
            onReact = { _, _ -> },
            onTogglePin = {},
            onNavigateToUserProfile = {}
        )
    }
}

private fun previewMessage(id: String, senderId: String, content: String, createdAt: String): Message =
    Message(
        localId = id,
        serverId = id,
        clientTempId = null,
        conversationId = "1",
        senderId = senderId,
        content = content,
        type = "text",
        createdAt = createdAt,
        updatedAt = null,
        isUnsent = false,
        isEdited = false,
        isDeleted = false,
        isPinned = false,
        pinnedBy = null,
        pinnedAt = null,
        replyTo = null,
        forwardedFrom = null,
        reactions = emptyList(),
        attachments = emptyList(),
        sendStatus = SendStatus.SENT
    )
