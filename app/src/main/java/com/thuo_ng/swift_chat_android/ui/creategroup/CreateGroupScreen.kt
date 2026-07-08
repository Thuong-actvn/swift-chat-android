package com.thuo_ng.swift_chat_android.ui.creategroup

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thuo_ng.swift_chat_android.domain.model.PublicUserProfile
import com.thuo_ng.swift_chat_android.ui.components.SwiftAvatar
import com.thuo_ng.swift_chat_android.ui.components.SwiftAvatarDefaults
import com.thuo_ng.swift_chat_android.ui.components.SwiftTextField

@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onNavigateToConversation: (String) -> Unit,
    viewModel: CreateGroupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreateGroupEffect.NavigateToConversation -> onNavigateToConversation(effect.conversationId)
                is CreateGroupEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    CreateGroupContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::handleIntent,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupContent(
    state: CreateGroupUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (CreateGroupIntent) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Group") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 16.dp).size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(
                            onClick = { onIntent(CreateGroupIntent.CreateClicked) },
                            enabled = state.canCreate
                        ) {
                            Text("Create", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            GroupInfoSection(
                title = state.title,
                onTitleChange = { onIntent(CreateGroupIntent.TitleChanged(it)) }
            )
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            Spacer(modifier = Modifier.height(8.dp))
            
            SwiftTextField(
                value = state.searchQuery,
                onValueChange = { onIntent(CreateGroupIntent.SearchQueryChanged(it)) },
                placeholder = "Search friends",
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = if (state.searchQuery.isNotBlank()) {
                    {
                        IconButton(onClick = { onIntent(CreateGroupIntent.SearchQueryChanged("")) }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear")
                        }
                    }
                } else null,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.isLoading && state.friends.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.filteredFriends, key = { it.id }) { friend ->
                        FriendSelectionRow(
                            friend = friend,
                            isSelected = state.selectedUserIds.contains(friend.id),
                            onSelect = { onIntent(CreateGroupIntent.UserSelected(friend.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupInfoSection(
    title: String,
    onTitleChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            SwiftAvatar(
                model = null,
                sizes = SwiftAvatarDefaults.Large,
                placeholderIcon = Icons.Default.Group
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        SwiftTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = "Group Name",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FriendSelectionRow(
    friend: PublicUserProfile,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SwiftAvatar(
            model = friend.avatarUrl,
            sizes = SwiftAvatarDefaults.Medium
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.displayLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
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
        }
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onSelect() }
        )
    }
}
