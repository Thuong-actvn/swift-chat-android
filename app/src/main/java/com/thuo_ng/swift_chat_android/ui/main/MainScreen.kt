package com.thuo_ng.swift_chat_android.ui.main

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.thuo_ng.swift_chat_android.ui.chat.ChatDetailScreen
import com.thuo_ng.swift_chat_android.ui.conversationinfo.ConversationInfoScreen
import com.thuo_ng.swift_chat_android.ui.conversations.ConversationListScreen
import com.thuo_ng.swift_chat_android.ui.friends.FriendsScreen
import com.thuo_ng.swift_chat_android.ui.friends.FriendsViewModel
import com.thuo_ng.swift_chat_android.ui.navigation.ChatDetail
import com.thuo_ng.swift_chat_android.ui.navigation.ConversationInfo
import com.thuo_ng.swift_chat_android.ui.navigation.Conversations
import com.thuo_ng.swift_chat_android.ui.navigation.EditProfile
import com.thuo_ng.swift_chat_android.ui.navigation.Friends
import com.thuo_ng.swift_chat_android.ui.navigation.Notifications
import com.thuo_ng.swift_chat_android.ui.navigation.PendingDirectChat
import com.thuo_ng.swift_chat_android.ui.navigation.Profile
import com.thuo_ng.swift_chat_android.ui.navigation.UserProfile
import com.thuo_ng.swift_chat_android.ui.notifications.NotificationScreen
import com.thuo_ng.swift_chat_android.ui.notifications.NotificationViewModel
import com.thuo_ng.swift_chat_android.ui.profile.ProfileScreen
import com.thuo_ng.swift_chat_android.ui.theme.SwiftChatTheme

private data class TabItem(
    val label: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector,
    val route: Any // @Serializable route object
)

private val tabs = listOf(
    TabItem("Chats", Icons.AutoMirrored.Outlined.Chat, Icons.AutoMirrored.Filled.Chat, Conversations),
    TabItem("Friends", Icons.Outlined.Group, Icons.Filled.Group, Friends()),
    TabItem("Notifications", Icons.Outlined.Notifications, Icons.Filled.Notifications, Notifications),
    TabItem("Profile", Icons.Outlined.Person, Icons.Filled.Person, Profile)
)

@Composable
fun MainScreen(
    rootNavController: NavController,
    onLogout: () -> Unit,
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    friendsViewModel: FriendsViewModel = hiltViewModel()
) {
    val mainNavController = rememberNavController()
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val notificationState by notificationViewModel.uiState.collectAsStateWithLifecycle()
    val friendsState by friendsViewModel.uiState.collectAsStateWithLifecycle()
    val notificationUnreadCount = notificationState.unreadCount
    val friendsRequestCount = friendsState.requestBadgeCount
    val showBottomBar = currentDestination == null || tabs.any { tab ->
        currentDestination.hierarchy.any { destination ->
            destination.hasRoute(tab.route::class)
        }
    }

    // Handle deep-link navigation requests coming back from UserProfileScreen
    // via rootNavController's SavedStateHandle.
    val rootCurrentEntry by rootNavController.currentBackStackEntryAsState()
    LaunchedEffect(rootCurrentEntry) {
        val handle = rootCurrentEntry?.savedStateHandle ?: return@LaunchedEffect
        handle.get<String>("navigate_to_chat")?.let { conversationId ->
            handle.remove<String>("navigate_to_chat")
            mainNavController.navigate(ChatDetail(conversationId))
        }
        val pendingId = handle.get<String>("navigate_to_pending_chat_id")
        if (pendingId != null) {
            val displayName = handle.get<String>("navigate_to_pending_chat_name") ?: pendingId
            val avatarUrl = handle.get<String>("navigate_to_pending_chat_avatar")
            handle.remove<String>("navigate_to_pending_chat_id")
            handle.remove<String>("navigate_to_pending_chat_name")
            handle.remove<String>("navigate_to_pending_chat_avatar")
            mainNavController.navigate(PendingDirectChat(pendingId, displayName, avatarUrl))
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .dropShadow(
                            shape = MaterialTheme.shapes.large,
                            shadow = Shadow(
                                radius = 16.dp,
                                spread = 0.dp,
                                color = Color.Black.copy(alpha = 0.08f),
                                offset = DpOffset(x = 0.dp, (-8).dp)
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.hasRoute(tab.route::class)
                            } == true

                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    mainNavController.navigate(tab.route) {
                                        popUpTo(mainNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            val badgeCount = when (tab.route) {
                                                is Friends -> friendsRequestCount
                                                Notifications -> notificationUnreadCount
                                                else -> 0
                                            }
                                            if (badgeCount > 0) {
                                                Badge { Text(formatBadgeCount(badgeCount)) }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.6f
                                    ),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.6f
                                    ),
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = mainNavController,
            startDestination = Conversations,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable<Conversations> {
                ConversationListScreen(
                    onConversationClick = { conversationId ->
                        mainNavController.navigate(ChatDetail(conversationId))
                    }
                )
            }
            composable<ChatDetail> { entry ->
                val route = entry.toRoute<ChatDetail>()
                ChatDetailScreen(
                    conversationId = route.conversationId,
                    onBack = { mainNavController.popBackStack() },
                    onOpenConversationInfo = { conversationId ->
                        mainNavController.navigate(ConversationInfo(conversationId))
                    },
                    onNavigateToUserProfile = { accountId ->
                        rootNavController.navigate(UserProfile(accountId))
                    }
                )
            }
            composable<PendingDirectChat> { entry ->
                val route = entry.toRoute<PendingDirectChat>()
                ChatDetailScreen(
                    pendingPartnerId = route.partnerId,
                    pendingDisplayName = route.displayName,
                    pendingAvatarUrl = route.avatarUrl,
                    onBack = { mainNavController.popBackStack() },
                    onOpenConversationInfo = { conversationId ->
                        mainNavController.navigate(ConversationInfo(conversationId))
                    },
                    onNavigateToUserProfile = { accountId ->
                        rootNavController.navigate(UserProfile(accountId))
                    }
                )
            }
            composable<ConversationInfo> { entry ->
                val route = entry.toRoute<ConversationInfo>()
                ConversationInfoScreen(
                    conversationId = route.conversationId,
                    onBack = { mainNavController.popBackStack() },
                    onConversationClosed = {
                        mainNavController.navigate(Conversations) {
                            popUpTo(Conversations) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToUserProfile = { accountId ->
                        rootNavController.navigate(UserProfile(accountId))
                    }
                )
            }
            composable<Friends> {
                FriendsScreen(
                    onOpenPendingDirectChat = { partnerId, displayName, avatarUrl ->
                        mainNavController.navigate(PendingDirectChat(partnerId, displayName, avatarUrl))
                    },
                    onOpenConversation = { conversationId ->
                        mainNavController.navigate(ChatDetail(conversationId))
                    },
                    onOpenPublicProfile = { accountId, _ ->
                        rootNavController.navigate(UserProfile(accountId))
                    },
                    viewModel = friendsViewModel
                )
            }
            composable<Notifications> {
                NotificationScreen(
                    onOpenConversation = { conversationId ->
                        mainNavController.navigate(ChatDetail(conversationId))
                    },
                    onNavigateToFriendsReceived = {
                        mainNavController.navigate(Friends(initialSection = "Received")) {
                            popUpTo(mainNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    viewModel = notificationViewModel
                )
            }
            composable<Profile> {
                ProfileScreen(
                    onNavigateToEditProfile = { rootNavController.navigate(EditProfile) },
                    onLogout = onLogout
                )
            }
        }
    }
}

private fun formatBadgeCount(count: Int): String {
    return if (count > 99) "99+" else count.toString()
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    SwiftChatTheme {
        MainScreen(
            rootNavController = rememberNavController(),
            onLogout = {}
        )
    }
}
