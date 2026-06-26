package com.thuo_ng.swift_chat_android.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.thuo_ng.swift_chat_android.ui.navigation.Calls
import com.thuo_ng.swift_chat_android.ui.navigation.Conversations
import com.thuo_ng.swift_chat_android.ui.navigation.Friends
import com.thuo_ng.swift_chat_android.ui.navigation.Profile

private data class TabItem(
    val label: String,
    val icon: ImageVector,
    val route: Any // @Serializable route object
)

private val tabs = listOf(
    TabItem("Chats", Icons.AutoMirrored.Outlined.Chat, Conversations),
    TabItem("Calls", Icons.Outlined.Call, Calls),
    TabItem("Friends", Icons.Outlined.Group, Friends),
    TabItem("Profile", Icons.Outlined.Person, Profile)
)

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val mainNavController = rememberNavController()
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
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
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = mainNavController,
            startDestination = Conversations,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Conversations> {
                PlaceholderTab(title = "Conversations", subtitle = "Chưa có cuộc trò chuyện nào!")
            }
            composable<Calls> {
                PlaceholderTab(title = "Calls", subtitle = "Chưa có cuộc gọi nào!")
            }
            composable<Friends> {
                PlaceholderTab(title = "Friends", subtitle = "Chưa có bạn bè!")
            }
            composable<Profile> {
                PlaceholderTab(title = "Profile", subtitle = "Thông tin cá nhân!")
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
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
