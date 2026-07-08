package com.thuo_ng.swift_chat_android.ui.friends

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thuo_ng.swift_chat_android.domain.model.BlockedUser
import com.thuo_ng.swift_chat_android.domain.model.FriendRequest
import com.thuo_ng.swift_chat_android.domain.model.FriendUser
import com.thuo_ng.swift_chat_android.domain.model.PublicUserProfile
import com.thuo_ng.swift_chat_android.domain.model.SearchUser
import com.thuo_ng.swift_chat_android.ui.components.SwiftAvatar
import com.thuo_ng.swift_chat_android.ui.components.SwiftAvatarDefaults
import com.thuo_ng.swift_chat_android.ui.components.SwiftTextField
import java.time.Duration
import java.time.Instant

@Composable
fun FriendsScreen(
    onOpenConversation: (String) -> Unit = {},
    onOpenPendingDirectChat: (String, String, String?) -> Unit = { _, _, _ -> },
    onOpenPublicProfile: (String, String) -> Unit = { _, _ -> },
    viewModel: FriendsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FriendsEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                is FriendsEffect.OpenConversation -> onOpenConversation(effect.conversationId)
                is FriendsEffect.OpenPendingDirectChat -> onOpenPendingDirectChat(
                    effect.partnerId,
                    effect.displayName,
                    effect.avatarUrl
                )
                is FriendsEffect.OpenPublicProfile -> {
                    onOpenPublicProfile(effect.accountId, effect.displayName)
                    snackbarHostState.showSnackbar("Profile screen for ${effect.displayName} is coming soon")
                }
            }
        }
    }

    FriendsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::handleIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendsContent(
    state: FriendsUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (FriendsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDialog by remember { mutableStateOf<PendingFriendDialog?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Friends",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    actions = {
                    }
                )
                SwiftTextField(
                    value = state.searchQuery,
                    onValueChange = { onIntent(FriendsIntent.SearchQueryChanged(it)) },
                    placeholder = when (state.selectedSection) {
                        FriendsSection.Friends -> "Search people"
                        FriendsSection.Received -> "Search received requests"
                        FriendsSection.Sent -> "Search sent requests"
                        FriendsSection.Blocked -> "Search blocked users"
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = if (state.searchQuery.isNotBlank()) {
                        {
                            IconButton(
                                onClick = {
                                    onIntent(FriendsIntent.SearchQueryChanged(""))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 10.dp)
                )
                FriendsTabs(
                    selectedSection = state.selectedSection,
                    requestCount = state.requestBadgeCount,
                    onSectionSelected = { onIntent(FriendsIntent.SectionSelected(it)) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(FriendsIntent.Refresh) },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                state.isLoading -> LoadingState()
                state.selectedSection == FriendsSection.Friends && state.isSearchMode -> SearchResultsList(
                    state = state,
                    onIntent = onIntent,
                    onUserClick = {
                        onIntent(
                            FriendsIntent.UserSelected(
                                accountId = it.id,
                                displayName = it.displayLabel,
                                avatarUrl = it.avatarUrl,
                                isFriend = it.isFriend == true
                            )
                        )
                    },
                    onConfirmRemove = { pendingDialog = PendingFriendDialog.Remove(it.id, it.displayLabel) },
                    onConfirmBlock = { pendingDialog = PendingFriendDialog.Block(it.id, it.displayLabel) }
                )
                else -> when (state.selectedSection) {
            FriendsSection.Friends -> FriendsList(
                state = state,
                onIntent = onIntent,
                onUserClick = {
                    onIntent(
                        FriendsIntent.UserSelected(
                            accountId = it.id,
                            displayName = it.displayLabel,
                            avatarUrl = it.avatarUrl,
                            isFriend = true
                        )
                    )
                },
                onConfirmRemove = { pendingDialog = PendingFriendDialog.Remove(it.id, it.displayLabel) },
                onConfirmBlock = { pendingDialog = PendingFriendDialog.Block(it.id, it.displayLabel) }
            )
                    FriendsSection.Received -> ReceivedRequestsList(
                        state = state,
                        onIntent = onIntent
                    )
                    FriendsSection.Sent -> SentRequestsList(
                        state = state,
                        onIntent = onIntent
                    )
                    FriendsSection.Blocked -> BlockedList(
                        state = state,
                        onIntent = onIntent
                    )
                }
            }
        }
    }

    pendingDialog?.let { dialog ->
        ConfirmFriendDialog(
            dialog = dialog,
            onDismiss = { pendingDialog = null },
            onConfirm = {
                when (dialog) {
                    is PendingFriendDialog.Remove -> onIntent(FriendsIntent.RemoveFriend(dialog.accountId))
                    is PendingFriendDialog.Block -> onIntent(FriendsIntent.BlockUser(dialog.accountId))
                }
                pendingDialog = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendsTabs(
    selectedSection: FriendsSection,
    requestCount: Int,
    onSectionSelected: (FriendsSection) -> Unit
) {
    val sections = FriendsSection.entries
    PrimaryScrollableTabRow(
        selectedTabIndex = sections.indexOf(selectedSection),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 16.dp,
        minTabWidth = 84.dp
    ) {
        sections.forEach { section ->
            Tab(
                selected = selectedSection == section,
                onClick = { onSectionSelected(section) },
                text = {
                    Text(
                        text = when (section) {
                            FriendsSection.Friends -> "Friends"
                            FriendsSection.Received -> if (requestCount > 0) "Received $requestCount" else "Received"
                            FriendsSection.Sent -> "Sent"
                            FriendsSection.Blocked -> "Blocked"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
private fun FriendsList(
    state: FriendsUiState,
    onIntent: (FriendsIntent) -> Unit,
    onUserClick: (PublicUserProfile) -> Unit,
    onConfirmRemove: (PublicUserProfile) -> Unit,
    onConfirmBlock: (PublicUserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.friends.isEmpty()) {
        CenteredState(
            icon = Icons.Outlined.Group,
            title = "No friends yet",
            message = "Find people to connect with."
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        items(
            items = state.friends,
            key = { it.id }
        ) { friend ->
            FriendRow(
                friend = friend,
                isRemoving = state.isActionLoading(FriendActionKeys.remove(friend.id)),
                isBlocking = state.isActionLoading(FriendActionKeys.block(friend.id)),
                isOpening = state.isActionLoading(FriendActionKeys.open(friend.id)),
                onClick = { onUserClick(friend) },
                onRemove = { onConfirmRemove(friend) },
                onBlock = { onConfirmBlock(friend) }
            )
        }

        if (state.hasMoreFriends) {
            item(key = "friends-load-more") {
                LaunchedEffect(state.nextFriendsOffset) {
                    onIntent(FriendsIntent.LoadMoreFriends)
                }
                LoadingMoreRow()
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    state: FriendsUiState,
    onIntent: (FriendsIntent) -> Unit,
    onUserClick: (SearchUser) -> Unit,
    onConfirmRemove: (SearchUser) -> Unit,
    onConfirmBlock: (SearchUser) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isSearching -> LoadingState(message = "Searching...")
        state.searchResults.isEmpty() -> CenteredState(
            icon = Icons.Outlined.Search,
            title = "No results",
            message = "Try another name or handle."
        )
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            items(
                items = state.searchResults,
                key = { it.id }
            ) { user ->
                SearchUserRow(
                    user = user,
                    state = state,
                    onIntent = onIntent,
                    onClick = { onUserClick(user) },
                    onConfirmRemove = { onConfirmRemove(user) },
                    onConfirmBlock = { onConfirmBlock(user) }
                )
            }
        }
    }
}

@Composable
private fun ReceivedRequestsList(
    state: FriendsUiState,
    onIntent: (FriendsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val query = state.searchQuery.trim()
    val requests = state.incomingRequests.filter { request ->
        request.sender.matchesQuery(query)
    }
    val hasAnyRequests = state.incomingRequests.isNotEmpty()

    if (requests.isEmpty()) {
        CenteredState(
            icon = if (query.isBlank()) Icons.Outlined.PersonAdd else Icons.Outlined.Search,
            title = if (query.isBlank() || !hasAnyRequests) "No received requests" else "No matching received requests",
            message = if (query.isBlank() || !hasAnyRequests) {
                "Friend requests sent to you will appear here."
            } else {
                "Try another name or handle."
            }
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = requests,
            key = { it.id }
        ) { request ->
            RequestRow(
                user = request.sender,
                createdAt = request.createdAt,
                primaryAction = RequestAction(
                    label = "Accept",
                    icon = Icons.Outlined.Check,
                    loading = state.isActionLoading(FriendActionKeys.accept(request.id)),
                    onClick = { onIntent(FriendsIntent.AcceptFriendRequest(request.id)) }
                ),
                secondaryAction = RequestAction(
                    label = "Reject",
                    icon = Icons.Outlined.Close,
                    loading = state.isActionLoading(FriendActionKeys.reject(request.id)),
                    onClick = { onIntent(FriendsIntent.RejectFriendRequest(request.id)) }
                ),
                onClick = {
                    onIntent(
                        FriendsIntent.UserSelected(
                            accountId = request.sender.id,
                            displayName = request.sender.displayLabel,
                            avatarUrl = request.sender.avatarUrl,
                            isFriend = false
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun SentRequestsList(
    state: FriendsUiState,
    onIntent: (FriendsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val query = state.searchQuery.trim()
    val requests = state.outgoingRequests.filter { request ->
        request.receiver.matchesQuery(query)
    }
    val hasAnyRequests = state.outgoingRequests.isNotEmpty()

    if (requests.isEmpty()) {
        CenteredState(
            icon = if (query.isBlank()) Icons.Outlined.PersonAdd else Icons.Outlined.Search,
            title = if (query.isBlank() || !hasAnyRequests) "No sent requests" else "No matching sent requests",
            message = if (query.isBlank() || !hasAnyRequests) {
                "Friend requests you send will appear here."
            } else {
                "Try another name or handle."
            }
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = requests,
            key = { it.id }
        ) { request ->
            RequestRow(
                user = request.receiver,
                createdAt = request.createdAt,
                primaryAction = RequestAction(
                    label = "Requested",
                    icon = Icons.Outlined.PersonAdd,
                    loading = false,
                    enabled = false,
                    onClick = {}
                ),
                secondaryAction = RequestAction(
                    label = "Cancel",
                    icon = Icons.Outlined.Cancel,
                    loading = state.isActionLoading(FriendActionKeys.cancel(request.id)),
                    onClick = { onIntent(FriendsIntent.CancelFriendRequest(request.id)) }
                ),
                onClick = {
                    onIntent(
                        FriendsIntent.UserSelected(
                            accountId = request.receiver.id,
                            displayName = request.receiver.displayLabel,
                            avatarUrl = request.receiver.avatarUrl,
                            isFriend = false
                        )
                    )
                }
            )
            }
        }
    }
@Composable
private fun BlockedList(
    state: FriendsUiState,
    onIntent: (FriendsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val query = state.searchQuery.trim()
    val blockedUsers = state.blockedUsers.filter { it.matchesQuery(query) }

    if (blockedUsers.isEmpty()) {
        CenteredState(
            icon = if (query.isBlank()) Icons.Outlined.Block else Icons.Outlined.Search,
            title = if (query.isBlank() || state.blockedUsers.isEmpty()) "No blocked users" else "No matching blocked users",
            message = if (query.isBlank() || state.blockedUsers.isEmpty()) {
                "People you block will appear here."
            } else {
                "Try another name or handle."
            }
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        items(
            items = blockedUsers,
            key = { it.id }
        ) { blockedUser ->
            BlockedUserRow(
                blockedUser = blockedUser,
                isLoading = state.isActionLoading(FriendActionKeys.unblock(blockedUser.id)),
                onUnblock = { onIntent(FriendsIntent.UnblockUser(blockedUser.id)) },
                onClick = {
                    onIntent(
                        FriendsIntent.UserSelected(
                            accountId = blockedUser.id,
                            displayName = blockedUser.displayLabel,
                            avatarUrl = blockedUser.avatarUrl,
                            isFriend = false
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun FriendRow(
    friend: PublicUserProfile,
    isRemoving: Boolean,
    isBlocking: Boolean,
    isOpening: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isOpening) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SwiftAvatar(
            model = friend.avatarUrl,
            sizes = SwiftAvatarDefaults.Medium,
            showOnlineIndicator = friend.isOnline == true
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.displayLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${friend.handle}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = friendStatusText(friend),
                style = MaterialTheme.typography.labelMedium,
                color = if (friend.isOnline == true) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                }
            )
        }
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                enabled = !isRemoving && !isBlocking && !isOpening
            ) {
                if (isRemoving || isBlocking || isOpening) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Friend actions"
                    )
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Remove friend") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.PersonRemove,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onRemove()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Block user") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Block,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onBlock()
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchUserRow(
    user: SearchUser,
    state: FriendsUiState,
    onIntent: (FriendsIntent) -> Unit,
    onClick: () -> Unit,
    onConfirmRemove: () -> Unit,
    onConfirmBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SwiftAvatar(
            model = user.avatarUrl,
            sizes = SwiftAvatarDefaults.Medium
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${user.handle}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        SearchUserActions(
            user = user,
            state = state,
            onIntent = onIntent,
            onConfirmRemove = onConfirmRemove,
            onConfirmBlock = onConfirmBlock
        )
    }
}

@Composable
private fun SearchUserActions(
    user: SearchUser,
    state: FriendsUiState,
    onIntent: (FriendsIntent) -> Unit,
    onConfirmRemove: () -> Unit,
    onConfirmBlock: () -> Unit
) {
    val outgoingRequest = state.outgoingRequestFor(user.id)
    val incomingRequest = state.incomingRequestFor(user.id)

    when {
        user.isFriend == true -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onConfirmRemove) {
                    Text("Friend")
                }
                IconButton(onClick = onConfirmBlock) {
                    Icon(
                        imageVector = Icons.Outlined.Block,
                        contentDescription = "Block user"
                    )
                }
            }
        }
        user.friendRequestStatus == "sent" -> {
            OutlinedButton(
                onClick = {
                    outgoingRequest?.let {
                        onIntent(FriendsIntent.CancelFriendRequest(it.id))
                    }
                },
                enabled = outgoingRequest != null && !state.isActionLoading(FriendActionKeys.cancel(outgoingRequest.id))
            ) {
                if (outgoingRequest != null && state.isActionLoading(FriendActionKeys.cancel(outgoingRequest.id))) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Cancel")
                }
            }
        }
        user.friendRequestStatus == "received" -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        incomingRequest?.let {
                            onIntent(FriendsIntent.AcceptFriendRequest(it.id))
                        }
                    },
                    enabled = incomingRequest != null && !state.isActionLoading(FriendActionKeys.accept(incomingRequest.id))
                ) {
                    if (incomingRequest != null && state.isActionLoading(FriendActionKeys.accept(incomingRequest.id))) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Accept")
                    }
                }
                OutlinedButton(
                    onClick = {
                        incomingRequest?.let {
                            onIntent(FriendsIntent.RejectFriendRequest(it.id))
                        }
                    },
                    enabled = incomingRequest != null && !state.isActionLoading(FriendActionKeys.reject(incomingRequest.id))
                ) {
                    Text("Reject")
                }
            }
        }
        else -> {
            val actionKey = FriendActionKeys.send(user.id)
            Button(
                onClick = { onIntent(FriendsIntent.SendFriendRequest(user.id)) },
                enabled = !state.isActionLoading(actionKey)
            ) {
                if (state.isActionLoading(actionKey)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Add")
                }
            }
        }
    }
}

@Composable
private fun RequestRow(
    user: FriendUser,
    createdAt: String,
    primaryAction: RequestAction,
    secondaryAction: RequestAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SwiftAvatar(
            model = user.avatarUrl,
            sizes = SwiftAvatarDefaults.Medium
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${user.handle}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = relativeTime(createdAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactActionButton(action = primaryAction, primary = true)
            CompactActionButton(action = secondaryAction, primary = false)
        }
    }
}

@Composable
private fun BlockedUserRow(
    blockedUser: BlockedUser,
    isLoading: Boolean,
    onUnblock: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SwiftAvatar(
            model = blockedUser.avatarUrl,
            sizes = SwiftAvatarDefaults.Medium
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = blockedUser.displayLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${blockedUser.handle}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedButton(
            onClick = onUnblock,
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Unblock")
            }
        }
    }
}

@Composable
private fun CompactActionButton(
    action: RequestAction,
    primary: Boolean,
    modifier: Modifier = Modifier
) {
    val content: @Composable () -> Unit = {
        if (action.loading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(action.label)
        }
    }

    if (primary) {
        Button(
            onClick = action.onClick,
            enabled = action.enabled && !action.loading,
            modifier = modifier.height(38.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            content()
        }
    } else {
        OutlinedButton(
            onClick = action.onClick,
            enabled = action.enabled && !action.loading,
            modifier = modifier.height(38.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier,
    message: String = "Loading friends..."
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LoadingMoreRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun CenteredState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
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
        }
    }
}

@Composable
private fun ConfirmFriendDialog(
    dialog: PendingFriendDialog,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isBlock = dialog is PendingFriendDialog.Block
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isBlock) Icons.Outlined.Block else Icons.Outlined.PersonOff,
                contentDescription = null
            )
        },
        title = {
            Text(if (isBlock) "Block ${dialog.name}?" else "Remove ${dialog.name}?")
        },
        text = {
            Text(
                if (isBlock) {
                    "Blocking removes existing friendship and pending requests."
                } else {
                    "This person will be removed from your friends list."
                }
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(if (isBlock) "Block" else "Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private data class RequestAction(
    val label: String,
    val icon: ImageVector,
    val loading: Boolean,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

private sealed class PendingFriendDialog(
    open val accountId: String,
    open val name: String
) {
    data class Remove(
        override val accountId: String,
        override val name: String
    ) : PendingFriendDialog(accountId, name)

    data class Block(
        override val accountId: String,
        override val name: String
    ) : PendingFriendDialog(accountId, name)
}

private fun friendStatusText(friend: PublicUserProfile): String {
    return when {
        friend.isOnline == true -> "Online"
        friend.lastSeen != null -> "Last seen ${relativeTime(friend.lastSeen)}"
        else -> "Offline"
    }
}

private fun FriendUser.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    return displayLabel.contains(query, ignoreCase = true) ||
        handle.contains(query, ignoreCase = true) ||
        id.contains(query, ignoreCase = true)
}

private fun BlockedUser.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    return displayLabel.contains(query, ignoreCase = true) ||
        handle.contains(query, ignoreCase = true) ||
        id.contains(query, ignoreCase = true)
}

private fun relativeTime(createdAt: String): String {
    val instant = runCatching { Instant.parse(createdAt) }.getOrNull() ?: return ""
    val duration = Duration.between(instant, Instant.now())
    val minutes = duration.toMinutes()
    val hours = duration.toHours()
    val days = duration.toDays()

    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}
