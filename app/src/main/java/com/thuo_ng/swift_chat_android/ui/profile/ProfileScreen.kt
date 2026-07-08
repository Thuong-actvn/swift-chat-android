package com.thuo_ng.swift_chat_android.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thuo_ng.swift_chat_android.domain.model.User
import com.thuo_ng.swift_chat_android.ui.components.FullScreenImageDialog
import com.thuo_ng.swift_chat_android.ui.components.CoverImageSection
import com.thuo_ng.swift_chat_android.ui.components.ProfileMenuItem
import com.thuo_ng.swift_chat_android.ui.components.ProfileStatItem
import com.thuo_ng.swift_chat_android.ui.components.SwiftAvatar
import com.thuo_ng.swift_chat_android.ui.components.SwiftAvatarDefaults
import com.thuo_ng.swift_chat_android.ui.theme.SwiftChatTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToEditProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileEffect.NavigateToEditProfile -> onNavigateToEditProfile()
                is ProfileEffect.NavigateToLogin -> onLogout()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = { /* TODO: Show menu */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            state.user?.let { user ->
                ProfileContent(
                    user = user,
                    friendCount = state.friendCount,
                    groupCount = state.groupCount,
                    mediaCount = state.mediaCount,
                    onEditClick = onNavigateToEditProfile,
                    onLogoutClick = { viewModel.handleIntent(ProfileIntent.LogoutClicked) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    user: User,
    friendCount: Int,
    groupCount: Int,
    mediaCount: Int,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit,
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
        // 1. Cover & Avatar Section
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            CoverImageSection(
                model = user.coverUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            // Curved background below cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 150.dp)
                    .height(80.dp) // Creates the space under the avatar
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
                    )
            )

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
                    showOnlineIndicator = true,
                    onClick = { showAvatarFullScreen = true }
                )

                Spacer(modifier = Modifier.weight(1f))

                androidx.compose.material3.OutlinedButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .width(80.dp)
                        .height(40.dp),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(0.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Profile",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Edit",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. User Info
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = user.displayedName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "@${user.handle}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!user.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = user.bio,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                user.location?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    user.website?.let { site ->
                        try {
                            val url = if (!site.startsWith("http://") && !site.startsWith("https://")) {
                                "https://$site"
                            } else {
                                site
                            }
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Handle error
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = "Website",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                user.website?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // 3. Stats (Real data)
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

        Spacer(modifier = Modifier.height(40.dp))

        // 4. Settings List
        Column {
            ProfileMenuItem(
                icon = Icons.Outlined.Lock,
                text = "Account Security",
                onClick = { /* TODO */ }
            )
            ProfileMenuItem(
                icon = Icons.Outlined.Notifications,
                text = "Notifications",
                onClick = { /* TODO */ }
            )
            ProfileMenuItem(
                icon = Icons.Outlined.PrivacyTip,
                text = "Privacy",
                onClick = { /* TODO */ }
            )
            ProfileMenuItem(
                icon = Icons.Outlined.DataUsage,
                text = "Data & Storage",
                onClick = { /* TODO */ }
            )
            ProfileMenuItem(
                icon = Icons.Outlined.Language,
                text = "Language",
                onClick = { /* TODO */ }
            )
            ProfileMenuItem(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                text = "Help & Support",
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileMenuItem(
                icon = Icons.AutoMirrored.Outlined.Logout,
                text = "Logout",
                onClick = onLogoutClick,
                contentColor = MaterialTheme.colorScheme.error,
                showArrow = false
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showAvatarFullScreen) {
        FullScreenImageDialog(
            model = user.avatarUrl,
            onDismissRequest = { showAvatarFullScreen = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileContentPreview() {
    SwiftChatTheme {
        Surface {
            ProfileContent(
                user = User(
                    id = "1",
                    username = "thuo_ng",
                    handle = "thuo_ng",
                    displayName = "Hoàng Văn Thương",
                    email = "thuo@example.com",
                    avatarUrl = null,
                    coverUrl = null,
                    bio = "Android Developer | Kotlin Enthusiast",
                    location = "Hanoi, Vietnam",
                    website = "https://github.com/thuo-ng",
                    lastSeen = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                ),
                friendCount = 120,
                groupCount = 15,
                mediaCount = 45,
                onEditClick = {},
                onLogoutClick = {}
            )
        }
    }
}
