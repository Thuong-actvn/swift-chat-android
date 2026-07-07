package com.thuo_ng.swift_chat_android.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.thuo_ng.swift_chat_android.ui.navigation.Conversations
import com.thuo_ng.swift_chat_android.ui.navigation.EditProfile
import com.thuo_ng.swift_chat_android.ui.navigation.Friends
import com.thuo_ng.swift_chat_android.ui.navigation.Notifications
import com.thuo_ng.swift_chat_android.ui.navigation.Profile
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
    TabItem("Friends", Icons.Outlined.Group, Icons.Filled.Group, Friends),
    TabItem("Notifications", Icons.Outlined.Notifications, Icons.Filled.Notifications, Notifications),
    TabItem("Profile", Icons.Outlined.Person, Icons.Filled.Person, Profile)
)

@Composable
fun MainScreen(
    rootNavController: NavController,
    onLogout: () -> Unit
) {
    val mainNavController = rememberNavController()
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
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
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
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
    ) { innerPadding ->
        NavHost(
            navController = mainNavController,
            startDestination = Conversations,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable<Conversations> {
                PlaceholderTab(title = "Conversations", subtitle = "Chưa có cuộc trò chuyện nào!")
            }
            composable<Friends> {
                PlaceholderTab(title = "Friends", subtitle = "Chưa có bạn bè!")
            }
            composable<Notifications> {
                PlaceholderTab(title = "Notifications", subtitle = "Chưa có thông báo nào!")
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

/**
 * Tab placeholder đơn giản — hiển thị tiêu đề + mô tả.
 */
@Composable
private fun PlaceholderTab(title: String, subtitle: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$title\n$subtitle",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
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
