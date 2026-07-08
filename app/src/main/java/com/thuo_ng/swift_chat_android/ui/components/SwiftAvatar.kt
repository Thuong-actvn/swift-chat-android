package com.thuo_ng.swift_chat_android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

@Immutable
data class SwiftAvatarSizes(
    val avatar: Dp,
    val onlineIndicator: Dp,
    val editBadge: Dp,
    val iconEdit: Dp
)

object SwiftAvatarDefaults {
    val Large = SwiftAvatarSizes(
        avatar = 96.dp,
        onlineIndicator = 20.dp,
        editBadge = 28.dp,
        iconEdit = 12.dp
    )
    
    val Medium = SwiftAvatarSizes(
        avatar = 48.dp,
        onlineIndicator = 12.dp,
        editBadge = 18.dp,
        iconEdit = 8.dp
    )

    val Small = SwiftAvatarSizes(
        avatar = 32.dp,
        onlineIndicator = 8.dp,
        editBadge = 12.dp,
        iconEdit = 6.dp
    )
}

@Composable
fun SwiftAvatar(
    model: Any?, // String URL or Uri
    modifier: Modifier = Modifier,
    sizes: SwiftAvatarSizes = SwiftAvatarDefaults.Medium,
    showOnlineIndicator: Boolean = false,
    showEditBadge: Boolean = false,
    placeholderIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Person,
    onClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.size(sizes.avatar)
    ) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = "Avatar",
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                .padding(2.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        ) {
            val state = painter.state
            if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error || state is AsyncImagePainter.State.Empty) {
                DefaultAvatarPlaceholder(
                    icon = placeholderIcon,
                    iconModifier = Modifier
                        .fillMaxSize()
                        .padding(sizes.avatar / 6)
                )
            } else {
                SubcomposeAsyncImageContent()
            }
        }

        if (showOnlineIndicator) {
            Box(
                modifier = Modifier
                    .size(sizes.onlineIndicator)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(2.dp)
                    .background(Color(0xFF4CAF50), CircleShape)
            )
        }

        if (showEditBadge) {
            Box(
                modifier = Modifier
                    .size(sizes.editBadge)
                    .align(Alignment.BottomEnd)
                    .offset(x = (2).dp, y = (2).dp)
                    .clip(CircleShape)
                    .then(if (onEditClick != null) Modifier.clickable { onEditClick() } else Modifier)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(2.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Avatar",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(sizes.iconEdit)
                )
            }
        }
    }
}

@Composable
fun DefaultAvatarPlaceholder(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Person,
    iconModifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = iconModifier,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}
