package com.thuo_ng.swift_chat_android.ui.conversationinfo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thuo_ng.swift_chat_android.domain.model.ConversationMember
import com.thuo_ng.swift_chat_android.domain.model.MuteDuration
import com.thuo_ng.swift_chat_android.ui.components.UserAvatar
import com.thuo_ng.swift_chat_android.ui.theme.Neutral100
import com.thuo_ng.swift_chat_android.ui.theme.Neutral90
import com.thuo_ng.swift_chat_android.ui.theme.Neutral94
import com.thuo_ng.swift_chat_android.ui.theme.NeutralVariant50
import com.thuo_ng.swift_chat_android.ui.theme.NeutralVariant80
import com.thuo_ng.swift_chat_android.ui.theme.NeutralVariant90
import com.thuo_ng.swift_chat_android.ui.theme.Red40
import coil.compose.AsyncImage

@Composable
fun ConversationInfoScreen(
    conversationId: String,
    onBack: () -> Unit,
    onConversationClosed: () -> Unit,
    viewModel: ConversationInfoViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingConfirm by remember { mutableStateOf<PendingConfirm?>(null) }

    LaunchedEffect(conversationId) {
        viewModel.handleIntent(ConversationInfoIntent.Start(conversationId))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ConversationInfoEffect.ConversationClosed -> onConversationClosed()
                is ConversationInfoEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ConversationInfoContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onMuteSwitchChanged = { viewModel.handleIntent(ConversationInfoIntent.MuteSwitchChanged(it)) },
        onAddMembersClick = { viewModel.handleIntent(ConversationInfoIntent.OpenAddMembers) },
        onViewAllMembersClick = { viewModel.handleIntent(ConversationInfoIntent.ShowAllMembers) },
        onDismissMuteDuration = { viewModel.handleIntent(ConversationInfoIntent.DismissMuteDurationSheet) },
        onMuteDurationSelected = { viewModel.handleIntent(ConversationInfoIntent.MuteDurationSelected(it)) },
        onDismissAddMembers = { viewModel.handleIntent(ConversationInfoIntent.DismissAddMembers) },
        onAddMemberQueryChanged = { viewModel.handleIntent(ConversationInfoIntent.AddMemberSearchChanged(it)) },
        onToggleAddMember = { viewModel.handleIntent(ConversationInfoIntent.ToggleAddMemberSelection(it)) },
        onSubmitAddMembers = { viewModel.handleIntent(ConversationInfoIntent.SubmitAddMembers) },
        onDismissAllMembers = { viewModel.handleIntent(ConversationInfoIntent.DismissAllMembers) },
        onAllMembersQueryChanged = { viewModel.handleIntent(ConversationInfoIntent.AllMembersSearchChanged(it)) },
        onChangeRole = { member, role ->
            viewModel.handleIntent(ConversationInfoIntent.ChangeMemberRole(member.accountId, role))
        },
        onConfirmAction = { pendingConfirm = it },
        onDeleteConversation = { viewModel.handleIntent(ConversationInfoIntent.DeleteConversation) },
        onLeaveGroup = { viewModel.handleIntent(ConversationInfoIntent.LeaveGroup) },
        onKickMember = { viewModel.handleIntent(ConversationInfoIntent.KickMember(it.accountId)) },
        onTransferLeadership = { viewModel.handleIntent(ConversationInfoIntent.TransferLeadership(it.accountId)) }
    )

    pendingConfirm?.let { confirm ->
        AlertDialog(
            onDismissRequest = { pendingConfirm = null },
            title = { Text(confirm.title) },
            text = { Text(confirm.message) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingConfirm = null
                        confirm.onConfirm()
                    },
                    colors = if (confirm.destructive) {
                        ButtonDefaults.buttonColors(containerColor = Red40)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(confirm.confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationInfoContent(
    state: ConversationInfoUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onMuteSwitchChanged: (Boolean) -> Unit,
    onAddMembersClick: () -> Unit,
    onViewAllMembersClick: () -> Unit,
    onDismissMuteDuration: () -> Unit,
    onMuteDurationSelected: (MuteDuration) -> Unit,
    onDismissAddMembers: () -> Unit,
    onAddMemberQueryChanged: (String) -> Unit,
    onToggleAddMember: (String) -> Unit,
    onSubmitAddMembers: () -> Unit,
    onDismissAllMembers: () -> Unit,
    onAllMembersQueryChanged: (String) -> Unit,
    onChangeRole: (ConversationMember, String) -> Unit,
    onConfirmAction: (PendingConfirm) -> Unit,
    onDeleteConversation: () -> Unit,
    onLeaveGroup: () -> Unit,
    onKickMember: (ConversationMember) -> Unit,
    onTransferLeadership: (ConversationMember) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (state.isLoading && state.conversation == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                )
            } else {
        LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    item {
                        ConversationInfoHeader(
                            state = state,
                            onBack = onBack
                        )
                    }
                    item {
                        ConversationActions(
                            isGroup = state.isGroup,
                            addEnabled = state.isGroup,
                            onAddMembersClick = onAddMembersClick,
                            modifier = Modifier.padding(top = 22.dp)
                        )
                    }
                    item {
                        SettingsCard(
                            isMuted = state.conversation?.currentParticipant?.isMuted == true,
                            muteEnabled = !state.isActionLoading(ConversationInfoActionKeys.Mute) &&
                                !state.isActionLoading(ConversationInfoActionKeys.Unmute),
                            onMuteSwitchChanged = onMuteSwitchChanged,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
                        )
                    }
                    item {
                        MediaSection(
                            state = state,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                    if (state.isGroup) {
                        item {
                            ParticipantsSection(
                                state = state,
                                onViewAllMembersClick = onViewAllMembersClick,
                                onChangeRole = onChangeRole,
                                onConfirmAction = onConfirmAction,
                                onKickMember = onKickMember,
                                onTransferLeadership = onTransferLeadership,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
                            )
                        }
                    }
                    item {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        DangerButton(
                            state = state,
                            onConfirmAction = onConfirmAction,
                            onDeleteConversation = onDeleteConversation,
                            onLeaveGroup = onLeaveGroup,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    if (state.showMuteDurationSheet) {
        MuteDurationSheet(
            onDismiss = onDismissMuteDuration,
            onDurationSelected = onMuteDurationSelected
        )
    }

    if (state.showAddMembersSheet) {
        AddMembersSheet(
            state = state,
            onDismiss = onDismissAddMembers,
            onQueryChanged = onAddMemberQueryChanged,
            onToggleCandidate = onToggleAddMember,
            onSubmit = onSubmitAddMembers
        )
    }

    if (state.showAllMembersSheet) {
        AllMembersSheet(
            state = state,
            onDismiss = onDismissAllMembers,
            onQueryChanged = onAllMembersQueryChanged,
            onChangeRole = onChangeRole,
            onConfirmAction = onConfirmAction,
            onKickMember = onKickMember,
            onTransferLeadership = onTransferLeadership
        )
    }
}

@Composable
private fun ConversationInfoHeader(
    state: ConversationInfoUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conversation = state.conversation
    val heroImageUrl = state.mediaPreviews.firstOrNull()?.url ?: conversation?.displayInfo?.avatarUrl
    val subtitle = buildString {
        if (state.isGroup) {
            append("${state.memberCount} Members")
        }
        if (conversation?.displayInfo?.isOnline == true || (state.isDirect && conversation?.displayInfo?.isOnline == true)) {
            if (isNotEmpty()) append(" • ")
            append("Online")
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(244.dp)
        ) {
            HeaderBanner(
                imageUrl = heroImageUrl,
                modifier = Modifier.matchParentSize()
            )
            TopOverlayBar(
                onBack = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 26.dp, vertical = 10.dp)
            )
            UserAvatar(
                avatarUrl = conversation?.displayInfo?.avatarUrl,
                size = 104.dp,
                showOnlineDot = state.isDirect,
                isOnline = conversation?.displayInfo?.isOnline == true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 46.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(58.dp))
        Text(
            text = conversation?.displayInfo?.title?.takeIf { it.isNotBlank() } ?: "Conversation",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 34.dp)
        )
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        val description = if (state.isGroup) {
            "Central hub for ${conversation?.displayInfo?.title ?: "this group"} discussions, file sharing, and updates."
        } else {
            null
        }
        if (description != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 44.dp)
            )
        }
    }
}

@Composable
private fun HeaderBanner(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.18f),
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.34f)
                            )
                        )
                    )
            )
        } else {
            HeaderFallbackArtwork(modifier = Modifier.fillMaxSize())
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(58.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )
    }
}

@Composable
private fun HeaderFallbackArtwork(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(92.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
        )
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 52.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            listOf(94.dp, 128.dp, 78.dp, 116.dp).forEach { height ->
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(height)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(3) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(width = 54.dp, height = 36.dp)
                ) {}
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 52.dp)
                .fillMaxWidth(0.68f)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.68f)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        )
    }
}

@Composable
private fun TopOverlayBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCircleButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            enabled = true,
            onClick = onBack
        )
        Spacer(modifier = Modifier.weight(1f))
        HeaderCircleButton(
            icon = Icons.Outlined.Search,
            contentDescription = "Search",
            enabled = false,
            onClick = {}
        )
        Spacer(modifier = Modifier.width(12.dp))
        HeaderCircleButton(
            icon = Icons.Filled.MoreVert,
            contentDescription = "More",
            enabled = false,
            onClick = {}
        )
    }
}

@Composable
private fun HeaderCircleButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Black.copy(alpha = if (enabled) 0.3f else 0.15f),
        modifier = Modifier.size(50.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = if (enabled) 1f else 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ConversationActions(
    isGroup: Boolean,
    addEnabled: Boolean,
    onAddMembersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundActionButton(
            icon = Icons.Outlined.Call,
            label = "Call",
            enabled = false,
            onClick = {}
        )
        Spacer(modifier = Modifier.width(20.dp))
        RoundActionButton(
            icon = Icons.Outlined.Videocam,
            label = "Video",
            enabled = false,
            onClick = {}
        )
        if (isGroup) {
            Spacer(modifier = Modifier.width(20.dp))
            RoundActionButton(
                icon = Icons.Outlined.PersonAdd,
                label = "Add",
                enabled = addEnabled,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = onAddMembersClick
            )
        }
    }
}

@Composable
private fun RoundActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
            ),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
            }
        )
    }
}

@Composable
private fun SettingsCard(
    isMuted: Boolean,
    muteEnabled: Boolean,
    onMuteSwitchChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            SettingSwitchRow(
                icon = Icons.Outlined.Notifications,
                label = "Mute Notifications",
                checked = isMuted,
                enabled = muteEnabled,
                onCheckedChange = onMuteSwitchChanged
            )
            SettingSwitchRow(
                icon = Icons.Outlined.PushPin,
                label = "Pin to Top",
                checked = true,
                enabled = true,
                onCheckedChange = {}
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
private fun MediaSection(
    state: ConversationInfoUiState,
    modifier: Modifier = Modifier
) {
    val previewSlots = state.mediaPreviews.take(2)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "MEDIA, LINKS, AND DOCS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "View All",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(2) { index ->
                val preview = previewSlots.getOrNull(index)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .size(96.dp)
                ) {
                    if (preview != null) {
                        AsyncImage(
                            model = preview.url,
                            contentDescription = "Shared media",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(29.dp)
                    )
                    Text(
                        text = "+${state.documentCount}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ParticipantsSection(
    state: ConversationInfoUiState,
    onViewAllMembersClick: () -> Unit,
    onChangeRole: (ConversationMember, String) -> Unit,
    onConfirmAction: (PendingConfirm) -> Unit,
    onKickMember: (ConversationMember) -> Unit,
    onTransferLeadership: (ConversationMember) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "PARTICIPANTS (${state.memberCount})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onViewAllMembersClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search participants",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        if (state.isRefreshingMembers && state.members.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        } else {
            state.displayedMembers.forEach { member ->
                MemberRow(
                    member = member,
                    currentRole = state.currentRole,
                    currentAccountId = state.currentAccountId,
                    onChangeRole = onChangeRole,
                    onConfirmAction = onConfirmAction,
                    onKickMember = onKickMember,
                    onTransferLeadership = onTransferLeadership
                )
            }
        }

        if (state.memberCount > 3) {
            TextButton(
                onClick = onViewAllMembersClick,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = "View all ${state.memberCount} members",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: ConversationMember,
    currentRole: String?,
    currentAccountId: String?,
    onChangeRole: (ConversationMember, String) -> Unit,
    onConfirmAction: (PendingConfirm) -> Unit,
    onKickMember: (ConversationMember) -> Unit,
    onTransferLeadership: (ConversationMember) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMe = member.accountId == currentAccountId
    val actions = remember(currentRole, member.role, currentAccountId, member.accountId) {
        availableMemberActions(
            currentRole = currentRole,
            targetRole = member.role,
            currentAccountId = currentAccountId,
            targetAccountId = member.accountId
        )
    }
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            avatarUrl = member.avatarUrl,
            size = 50.dp
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isMe) "You" else member.displayLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${member.handle}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier.width(132.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(86.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                RoleBadge(member.role)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                if (actions.isNotEmpty()) {
                    Surface(
                        onClick = { menuExpanded = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Member actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        actions.forEach { action ->
                            val isDestructive = action == ConversationMemberAction.Kick
                            val actionColor = if (isDestructive) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = action.labelFor(member),
                                        color = if (isDestructive) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = action.icon(),
                                        contentDescription = null,
                                        tint = actionColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    when (action) {
                                        ConversationMemberAction.Kick -> onConfirmAction(
                                            PendingConfirm(
                                                title = "Remove member?",
                                                message = "Remove ${member.displayLabel} from this group.",
                                                confirmLabel = "Remove",
                                                destructive = true,
                                                onConfirm = { onKickMember(member) }
                                            )
                                        )
                                        ConversationMemberAction.PromoteToDeputy -> onChangeRole(member, "deputy")
                                        ConversationMemberAction.DemoteToMember -> onChangeRole(member, "member")
                                        ConversationMemberAction.TransferLeadership -> onConfirmAction(
                                            PendingConfirm(
                                                title = "Transfer leadership?",
                                                message = "${member.displayLabel} will become the group leader.",
                                                confirmLabel = "Transfer",
                                                destructive = false,
                                                onConfirm = { onTransferLeadership(member) }
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(role: String) {
    val normalized = role.normalizedRole()
    val background = when (normalized) {
        "leader" -> Color(0xFFCDEBFF)
        "deputy" -> Neutral90
        else -> Color.Transparent
    }
    val content = when (normalized) {
        "leader" -> MaterialTheme.colorScheme.primary
        "deputy" -> NeutralVariant50
        else -> Color.Transparent
    }
    val label = when (normalized) {
        "leader" -> "Leader"
        "deputy" -> "Deputy"
        else -> ""
    }

    if (label.isNotBlank()) {
        Surface(
            color = background,
            shape = RoundedCornerShape(7.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = content,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(68.dp)
                    .padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun DangerButton(
    state: ConversationInfoUiState,
    onConfirmAction: (PendingConfirm) -> Unit,
    onDeleteConversation: () -> Unit,
    onLeaveGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when {
        state.isDirect -> "Hide Conversation"
        canDisbandGroup(state.currentRole) -> "Disband Group"
        else -> "Leave Group"
    }
    val icon = when {
        state.isDirect -> Icons.Outlined.Delete
        canDisbandGroup(state.currentRole) -> Icons.Outlined.Delete
        else -> Icons.AutoMirrored.Outlined.ExitToApp
    }
    val isLoading = state.isActionLoading(ConversationInfoActionKeys.DeleteConversation) ||
        state.isActionLoading(ConversationInfoActionKeys.LeaveGroup)

    Button(
        onClick = {
            val isLeave = state.isGroup && canLeaveGroup(state.currentRole)
            onConfirmAction(
                PendingConfirm(
                    title = "$label?",
                    message = when {
                        state.isDirect -> "Hide this direct conversation from your chat list."
                        isLeave -> "You will leave this group and stop receiving messages."
                        else -> "This group will be disbanded for every member."
                    },
                    confirmLabel = label,
                    destructive = true,
                    onConfirm = {
                        if (isLeave) onLeaveGroup() else onDeleteConversation()
                    }
                )
            )
        },
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFF1F1),
            contentColor = Color(0xFFD32F2F),
            disabledContainerColor = Neutral94,
            disabledContentColor = NeutralVariant50
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = null,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFFD32F2F))
        } else {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuteDurationSheet(
    onDismiss: () -> Unit,
    onDurationSelected: (MuteDuration) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Text(
                text = "Mute notifications",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            MuteDuration.entries.forEach { duration ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onDurationSelected(duration) }
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = duration.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMembersSheet(
    state: ConversationInfoUiState,
    onDismiss: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onToggleCandidate: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val candidates = remember(
        state.friends,
        state.addMemberSearchResults,
        state.addMemberSearchQuery,
        state.members,
        state.currentAccountId
    ) {
        state.addMemberCandidates()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add members",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.selectedAddMemberIds.size} selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = state.addMemberSearchQuery,
                onValueChange = onQueryChanged,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Search people") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(352.dp)
            ) {
                when {
                    state.isLoadingFriends && candidates.isEmpty() -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                    candidates.isEmpty() -> Text(
                        text = "No people to add",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidates, key = { it.accountId }) { candidate ->
                            AddMemberCandidateRow(
                                candidate = candidate,
                                selected = candidate.accountId in state.selectedAddMemberIds,
                                onClick = { onToggleCandidate(candidate.accountId) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSubmit,
                enabled = state.selectedAddMemberIds.isNotEmpty() &&
                    !state.isActionLoading(ConversationInfoActionKeys.AddMembers),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isActionLoading(ConversationInfoActionKeys.AddMembers)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (state.selectedAddMemberIds.isEmpty()) {
                            "Select members"
                        } else {
                            "Add ${state.selectedAddMemberIds.size} selected"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMemberCandidateRow(
    candidate: AddMemberCandidate,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            onClick = onClick,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            shape = RoundedCornerShape(18.dp),
            border = if (selected) {
                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f))
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(avatarUrl = candidate.avatarUrl, size = 48.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = candidate.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@${candidate.handle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Checkbox(checked = selected, onCheckedChange = { onClick() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllMembersSheet(
    state: ConversationInfoUiState,
    onDismiss: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onChangeRole: (ConversationMember, String) -> Unit,
    onConfirmAction: (PendingConfirm) -> Unit,
    onKickMember: (ConversationMember) -> Unit,
    onTransferLeadership: (ConversationMember) -> Unit
) {
    val members = remember(state.members, state.allMembersSearchQuery) {
        state.members.filteredBy(state.allMembersSearchQuery)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Text(
                text = "Participants",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = state.allMembersSearchQuery,
                onValueChange = onQueryChanged,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Search members") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(members, key = { it.accountId }) { member ->
                    MemberRow(
                        member = member,
                        currentRole = state.currentRole,
                        currentAccountId = state.currentAccountId,
                        onChangeRole = onChangeRole,
                        onConfirmAction = onConfirmAction,
                        onKickMember = onKickMember,
                        onTransferLeadership = onTransferLeadership
                    )
                    HorizontalDivider(color = NeutralVariant80.copy(alpha = 0.35f))
                }
            }
        }
    }
}

private fun ConversationMemberAction.labelFor(member: ConversationMember): String =
    when (this) {
        ConversationMemberAction.Kick -> "Remove"
        ConversationMemberAction.PromoteToDeputy -> "Make deputy"
        ConversationMemberAction.DemoteToMember -> "Make member"
        ConversationMemberAction.TransferLeadership -> "Make ${member.displayLabel} leader"
    }

private fun ConversationMemberAction.icon(): ImageVector =
    when (this) {
        ConversationMemberAction.Kick -> Icons.Outlined.Delete
        ConversationMemberAction.PromoteToDeputy -> Icons.Outlined.Shield
        ConversationMemberAction.DemoteToMember -> Icons.Outlined.Group
        ConversationMemberAction.TransferLeadership -> Icons.Outlined.Shield
    }

private fun ConversationInfoUiState.addMemberCandidates(): List<AddMemberCandidate> {
    val existingIds = members.map { it.accountId }.toSet() + listOfNotNull(currentAccountId)
    val queryIsBlank = addMemberSearchQuery.isBlank()
    val candidates = if (queryIsBlank) {
        friends.map {
            AddMemberCandidate(
                accountId = it.id,
                handle = it.handle,
                displayName = it.displayLabel,
                avatarUrl = it.avatarUrl,
                isFriend = true
            )
        }
    } else {
        addMemberSearchResults.map {
            AddMemberCandidate(
                accountId = it.id,
                handle = it.handle,
                displayName = it.displayLabel,
                avatarUrl = it.avatarUrl,
                isFriend = it.isFriend == true
            )
        }
    }

    return candidates
        .filter { it.accountId.isNotBlank() && it.accountId !in existingIds }
        .distinctBy { it.accountId }
        .sortedByDescending { it.isFriend }
}

private fun List<ConversationMember>.filteredBy(query: String): List<ConversationMember> {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return this
    return filter { member ->
        member.displayLabel.contains(trimmed, ignoreCase = true) ||
            member.handle.contains(trimmed, ignoreCase = true) ||
            member.accountId.contains(trimmed, ignoreCase = true)
    }
}

data class PendingConfirm(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val destructive: Boolean,
    val onConfirm: () -> Unit
)
