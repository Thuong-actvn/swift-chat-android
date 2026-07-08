package com.thuo_ng.swift_chat_android.ui.profile.edit


import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.thuo_ng.swift_chat_android.core.util.rememberImagePicker
import com.thuo_ng.swift_chat_android.ui.components.FullScreenImageDialog
import com.thuo_ng.swift_chat_android.ui.components.SwiftTextField
import com.thuo_ng.swift_chat_android.ui.components.CoverImageSection
import com.thuo_ng.swift_chat_android.ui.components.SwiftAvatar
import com.thuo_ng.swift_chat_android.ui.components.SwiftAvatarDefaults
import com.thuo_ng.swift_chat_android.ui.theme.SwiftChatTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Bottom Sheet State cho việc chọn ảnh bìa
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    val avatarPicker = rememberImagePicker { uri ->
        viewModel.handleIntent(EditProfileIntent.AvatarSelected(uri))
    }

    val coverPicker = rememberImagePicker { uri ->
        viewModel.handleIntent(EditProfileIntent.CoverSelected(uri))
        showBottomSheet = false
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is EditProfileEffect.NavigateBack -> onNavigateBack()
                is EditProfileEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit profile", style = MaterialTheme.typography.headlineSmall)},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.handleIntent(EditProfileIntent.SaveProfile) },
                        enabled = !state.isSaving
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            EditProfileContent(
                state = state,
                onIntent = viewModel::handleIntent,
                onAvatarClick = {avatarPicker.pickFromGallery()},
                onCoverClick = { showBottomSheet = true }
            )

            if (state.isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = false) {}, // Block touches
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    ListItem(
                        headlineContent = { Text("Takes a photo") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                        modifier = Modifier.clickable { coverPicker.takePhoto() }
                    )
                    ListItem(
                        headlineContent = { Text("Pick from gallery") },
                        leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                        modifier = Modifier.clickable { coverPicker.pickFromGallery() }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditProfileContent(
    state: EditProfileUiState,
    onIntent: (EditProfileIntent) -> Unit,
    onAvatarClick: () -> Unit,
    onCoverClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showAvatarFullScreen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // 1. Cover & Avatar Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            CoverImageSection(
                model = state.pendingCoverUri ?: state.coverUrl,
                onCameraClick = onCoverClick,
                modifier = Modifier.height(200.dp)
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp)
            ) {
                SwiftAvatar(
                    model = state.pendingAvatarUri ?: state.avatarUrl,
                    sizes = SwiftAvatarDefaults.Large,
                    showEditBadge = true,
                    onClick = { showAvatarFullScreen = true },
                    onEditClick = onAvatarClick
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Form Fields
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text("DISPLAY NAME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            SwiftTextField(
                value = state.displayName,
                onValueChange = { onIntent(EditProfileIntent.DisplayNameChanged(it)) },
                placeholder = "Display name"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("USERNAME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            SwiftTextField(
                value = state.handle,
                onValueChange = { onIntent(EditProfileIntent.HandleChanged(it)) },
                placeholder = "Username",
                leadingIcon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("BIO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            SwiftTextField(
                value = state.bio,
                onValueChange = { onIntent(EditProfileIntent.BioChanged(it)) },
                placeholder = "Tell us about yourself",
                modifier = Modifier.height(100.dp),
                singleLine = false,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("WEBSITE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            SwiftTextField(
                value = state.website,
                onValueChange = { onIntent(EditProfileIntent.WebsiteChanged(it)) },
                placeholder = "https://yourwebsite.com",
                leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("LOCATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            SwiftTextField(
                value = state.location,
                onValueChange = { onIntent(EditProfileIntent.LocationChanged(it)) },
                placeholder = "City, Country",
                leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    if (showAvatarFullScreen) {
        FullScreenImageDialog(
            model = state.pendingAvatarUri ?: state.avatarUrl,
            onDismissRequest = { showAvatarFullScreen = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditProfileContentPreview() {
    SwiftChatTheme {
        Surface {
            EditProfileContent(
                state = EditProfileUiState(
                    displayName = "Thuo Ng",
                    handle = "thuo_ng",
                    bio = "Android Developer | Kotlin Enthusiast",
                    website = "https://github.com/thuo-ng",
                    location = "Hanoi, Vietnam"
                ),
                onIntent = {},
                onAvatarClick = {},
                onCoverClick = {}
            )
        }
    }
}
