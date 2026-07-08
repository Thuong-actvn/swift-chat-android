package com.thuo_ng.swift_chat_android.ui.userprofile

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.thuo_ng.swift_chat_android.domain.model.PublicUserProfile
import com.thuo_ng.swift_chat_android.ui.components.CoverImageSection
import com.thuo_ng.swift_chat_android.ui.components.FullScreenImageDialog
import com.thuo_ng.swift_chat_android.ui.components.ProfileStatItem
import com.thuo_ng.swift_chat_android.ui.components.SwiftAvatar
import com.thuo_ng.swift_chat_android.ui.components.SwiftAvatarDefaults

@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onNavigateToChat: (conversationId: String) -> Unit,
    onNavigateToPendingChat: (partnerId: String, displayName: String, avatarUrl: String?) -> Unit,
    onNavigateToEditProfile: () -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.handleIntent(UserProfileIntent.Load(userId))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is UserProfileEffect.NavigateToChat -> onNavigateToChat(effect.conversationId)
                is UserProfileEffect.NavigateToPendingChat -> onNavigateToPendingChat(
                    effect.partnerId, effect.displayName, effect.avatarUrl
                )
                UserProfileEffect.NavigateBack -> onBack()
                UserProfileEffect.NavigateToEditProfile -> onNavigateToEditProfile()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null && state.user == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Could not load profile",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = onBack) { Text("Go back") }
                        }
                    }
                }

                state.user != null -> {
                    UserProfileContent(
                        user = state.user!!,
                        selectedTab = state.selectedTab,
                        isCurrentUser = state.isCurrentUser,
                        friendCount = state.friendCount,
                        groupCount = state.groupCount,
                        mediaCount = state.mediaCount,
                        onBack = onBack,
                        onMessageClick = {
                            if (state.isCurrentUser) {
                                viewModel.handleIntent(UserProfileIntent.EditProfileClicked)
                            } else {
                                viewModel.handleIntent(UserProfileIntent.MessageClicked)
                            }
                        },
                        onAudioCallClick = { viewModel.handleIntent(UserProfileIntent.AudioCallClicked) },
                        onVideoCallClick = { viewModel.handleIntent(UserProfileIntent.VideoCallClicked) },
                        onMoreClick = { viewModel.handleIntent(UserProfileIntent.MoreClicked) },
                        onTabSelected = { viewModel.handleIntent(UserProfileIntent.TabSelected(it)) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UserProfileContent(
    user: PublicUserProfile,
    selectedTab: UserProfileTab,
    isCurrentUser: Boolean,
    friendCount: Int,
    groupCount: Int,
    mediaCount: Int,
    onBack: () -> Unit,
    onMessageClick: () -> Unit,
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onMoreClick: () -> Unit,
    onTabSelected: (UserProfileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showAvatarFullScreen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // ── 1. Cover + Avatar + back/settings overlay ───────────────
        Box(modifier = Modifier.fillMaxWidth()) {
            // Cover image
            CoverImageSection(
                model = user.coverUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            // Rounded white card beneath cover that bleeds into content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 150.dp)
                    .height(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
                    )
            )

            // Top overlay: back + settings buttons
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CircleIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack
                )
                CircleIconButton(
                    icon = if (isCurrentUser) Icons.Filled.Settings else Icons.Filled.MoreVert,
                    contentDescription = if (isCurrentUser) "Settings" else "More",
                    onClick = onMoreClick
                )
            }

            // Avatar row with online indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 160.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                SwiftAvatar(
                    model = user.avatarUrl,
                    sizes = SwiftAvatarDefaults.Large,
                    showOnlineIndicator = user.isOnline == true,
                    onClick = { showAvatarFullScreen = true }
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 2. User info ────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = user.displayLabel,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${user.handle}",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!user.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = user.bio,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
            }

            // Location & website
            if (!user.location.isNullOrBlank() || !user.website.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                if (!user.location.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = user.location,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
                if (!user.website.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            try {
                                val url = if (!user.website.startsWith("http://") && !user.website.startsWith("https://")) {
                                    "https://${user.website}"
                                } else {
                                    user.website
                                }
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Handle error
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = "Website",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = user.website,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // ── 3. Stats row ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 16.dp)
        ) {
            ProfileStatItem(
                value = friendCount.toString(),
                label = "Friends",
                modifier = Modifier.weight(1f)
            )
            ProfileStatItem(
                value = groupCount.toString(),
                label = "Groups",
                modifier = Modifier.weight(1f)
            )
            ProfileStatItem(
                value = mediaCount.toString(),
                label = "Media",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 4. Action buttons ────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Message – primary action
            Button(
                onClick = onMessageClick,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isCurrentUser) Icons.Filled.Settings else Icons.AutoMirrored.Filled.Message,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCurrentUser) "Edit Profile" else "Message",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            if (!isCurrentUser) {
                // Audio call
                ActionCircleButton(
                    icon = Icons.Outlined.Call,
                    contentDescription = "Audio call",
                    onClick = onAudioCallClick
                )

                // Video call
                ActionCircleButton(
                    icon = Icons.Outlined.Videocam,
                    contentDescription = "Video call",
                    onClick = onVideoCallClick
                )

                // More
                ActionCircleButton(
                    icon = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    onClick = onMoreClick
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── 5. Media / Files / Links tabs + content ─────────────────
        MediaTabSection(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Full-screen avatar dialog
    if (showAvatarFullScreen) {
        FullScreenImageDialog(
            model = user.avatarUrl,
            onDismissRequest = { showAvatarFullScreen = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab + Media / Files / Links section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MediaTabSection(
    selectedTab: UserProfileTab,
    onTabSelected: (UserProfileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Tab row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            UserProfileTab.entries.forEach { tab ->
                TabLabel(
                    text = tab.label,
                    isSelected = tab == selectedTab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content area per tab – placeholder / empty for now
        when (selectedTab) {
            UserProfileTab.MEDIA -> MediaPlaceholder()
            UserProfileTab.FILES -> FilesPlaceholder()
            UserProfileTab.LINKS -> LinksPlaceholder()
        }
    }
}

@Composable
private fun TabLabel(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        animationSpec = tween(200),
        label = "tab_color"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Underline indicator
        Box(
            modifier = Modifier
                .height(2.5.dp)
                .width(if (isSelected) 28.dp else 0.dp)
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(50)
                )
        )
    }
}

// ── Placeholder composables ─────────────────────────────────────────────────

@Composable
private fun MediaPlaceholder() {
    // 3-column grid of skeleton tiles
    val placeholderCount = 6
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(placeholderCount / 3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun FilesPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(16.dp)
                    )
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No files yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun LinksPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(16.dp)
                    )
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No links yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable small composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.28f),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ActionCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
