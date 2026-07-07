package com.thuo_ng.swift_chat_android.ui.conversations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thuo_ng.swift_chat_android.R
import com.thuo_ng.swift_chat_android.domain.model.Conversation
import com.thuo_ng.swift_chat_android.domain.model.CurrentParticipant
import com.thuo_ng.swift_chat_android.domain.model.DisplayInfo
import com.thuo_ng.swift_chat_android.domain.model.MessagePreview
import com.thuo_ng.swift_chat_android.ui.conversations.components.ConversationItem
import com.thuo_ng.swift_chat_android.ui.conversations.components.ConversationShimmer
import com.thuo_ng.swift_chat_android.ui.theme.SwiftChatTheme

@Composable
fun ConversationListScreen(
    onConversationClick: (String) -> Unit,
    onSearchClick: () -> Unit = {},
    onNewConversationClick: () -> Unit = {},
    viewModel: ConversationListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ConversationEffect.NavigateToConversation -> onConversationClick(effect.conversationId)
                is ConversationEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ConversationListContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onFilterChange = { viewModel.handleIntent(ConversationIntent.FilterChanged(it)) },
        onRefresh = { viewModel.handleIntent(ConversationIntent.Refresh) },
        onConversationClick = {
            viewModel.handleIntent(ConversationIntent.ConversationClicked(it))
        },
        onSearchClick = onSearchClick,
        onNewConversationClick = onNewConversationClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationListContent(
    state: ConversationUiState,
    snackbarHostState: SnackbarHostState,
    onFilterChange: (ConversationFilter) -> Unit,
    onRefresh: () -> Unit,
    onConversationClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onNewConversationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            androidx.compose.foundation.layout.Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo_app),
                                    contentDescription = "SwiftChat app icon",
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SwiftChat",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search"
                            )
                        }
                        IconButton(onClick = onNewConversationClick) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "New conversation"
                            )
                        }
                    }
                )
                ConversationTabs(
                    selectedFilter = state.selectedFilter,
                    onFilterChange = onFilterChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                state.isLoading -> ConversationLoadingList()
                state.errorMessage != null && state.conversations.isEmpty() -> ConversationErrorContent(
                    message = state.errorMessage,
                    onRetry = onRefresh
                )
                state.visibleConversations.isEmpty() -> ConversationEmptyContent(
                    filter = state.selectedFilter
                )
                else -> ConversationList(
                    conversations = state.visibleConversations,
                    onConversationClick = onConversationClick
                )
            }
        }
    }
}

@Composable
private fun ConversationTabs(
    selectedFilter: ConversationFilter,
    onFilterChange: (ConversationFilter) -> Unit
) {
    val filters = ConversationFilter.entries

    PrimaryTabRow(
        selectedTabIndex = filters.indexOf(selectedFilter),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        filters.forEach { filter ->
            Tab(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                text = {
                    Text(
                        text = when (filter) {
                            ConversationFilter.Chats -> "Chats"
                            ConversationFilter.Groups -> "Groups"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun ConversationList(
    conversations: List<Conversation>,
    onConversationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        items(
            items = conversations,
            key = { it.id }
        ) { conversation ->
            ConversationItem(
                conversation = conversation,
                onClick = { onConversationClick(conversation.id) }
            )
        }
    }
}

@Composable
private fun ConversationLoadingList(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        items(6) {
            ConversationShimmer()
        }
    }
}

@Composable
private fun ConversationErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConversationCenteredState(
        icon = Icons.AutoMirrored.Outlined.Chat,
        title = "Could not load conversations",
        message = message,
        modifier = modifier,
        action = {
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    )
}

@Composable
private fun ConversationEmptyContent(
    filter: ConversationFilter,
    modifier: Modifier = Modifier
) {
    ConversationCenteredState(
        icon = when (filter) {
            ConversationFilter.Chats -> Icons.AutoMirrored.Outlined.Chat
            ConversationFilter.Groups -> Icons.Outlined.Group
        },
        title = when (filter) {
            ConversationFilter.Chats -> "No chats yet"
            ConversationFilter.Groups -> "No groups yet"
        },
        message = when (filter) {
            ConversationFilter.Chats -> "New messages will appear here."
            ConversationFilter.Groups -> "Group conversations will appear here."
        },
        modifier = modifier
    )
}

@Composable
private fun ConversationCenteredState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (action != null) {
                Spacer(modifier = Modifier.height(16.dp))
                action()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationListContentPreview() {
    SwiftChatTheme {
        ConversationListContent(
            state = ConversationUiState(
                isLoading = false,
                conversations = previewConversations()
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onFilterChange = {},
            onRefresh = {},
            onConversationClick = {},
            onSearchClick = {},
            onNewConversationClick = {}
        )
    }
}

private fun previewConversations(): List<Conversation> {
    return listOf(
        Conversation(
            id = "direct-1",
            type = "direct",
            displayInfo = DisplayInfo(
                title = "Linh Tran",
                avatarUrl = null,
                isOnline = true
            ),
            createdAt = "2026-07-07T02:00:00Z",
            updatedAt = "2026-07-07T04:10:00Z",
            unreadCount = 3,
            currentParticipant = CurrentParticipant(
                role = "member",
                isMuted = false,
                mutedUntil = null,
                lastReadMessageId = null
            ),
            participantPreview = emptyList(),
            totalParticipants = 2,
            lastMessage = MessagePreview(
                id = "message-1",
                content = "Hen gap luc 8h nhe.",
                senderId = "user-2",
                senderName = "Linh",
                timestamp = "2026-07-07T04:10:00Z",
                type = "text"
            )
        ),
        Conversation(
            id = "group-1",
            type = "group",
            displayInfo = DisplayInfo(
                title = "Android Team",
                avatarUrl = null,
                isOnline = null
            ),
            createdAt = "2026-07-05T02:00:00Z",
            updatedAt = "2026-07-06T14:30:00Z",
            unreadCount = 0,
            currentParticipant = null,
            participantPreview = emptyList(),
            totalParticipants = 5,
            lastMessage = MessagePreview(
                id = "message-2",
                content = "API conversations da san sang.",
                senderId = "user-3",
                senderName = "Minh",
                timestamp = "2026-07-06T14:30:00Z",
                type = "text"
            )
        )
    )
}
