package com.thuo_ng.swift_chat_android.ui.main

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.thuo_ng.swift_chat_android.ui.navigation.AuthGraph
import com.thuo_ng.swift_chat_android.ui.navigation.EditProfile
import com.thuo_ng.swift_chat_android.ui.navigation.MainGraph
import com.thuo_ng.swift_chat_android.ui.navigation.UserProfile
import com.thuo_ng.swift_chat_android.ui.navigation.CreateGroup
import com.thuo_ng.swift_chat_android.ui.profile.edit.EditProfileScreen
import com.thuo_ng.swift_chat_android.ui.userprofile.UserProfileScreen
import com.thuo_ng.swift_chat_android.ui.creategroup.CreateGroupScreen


fun NavGraphBuilder.mainGraph(rootNavController: NavController) {
    composable<MainGraph> {
        MainScreen(
            rootNavController = rootNavController,
            onLogout = {
                rootNavController.navigate(AuthGraph) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }

    composable<EditProfile> {
        EditProfileScreen(
            onNavigateBack = { rootNavController.popBackStack() }
        )
    }

    composable<UserProfile> { entry ->
        val route = entry.toRoute<UserProfile>()
        UserProfileScreen(
            userId = route.userId,
            onBack = { rootNavController.popBackStack() },
            onNavigateToChat = { conversationId ->
                // Pop UserProfile so MainGraph is on top, then signal via savedStateHandle
                if (rootNavController.popBackStack()) {
                    rootNavController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("navigate_to_chat", conversationId)
                }
            },
            onNavigateToPendingChat = { partnerId, displayName, avatarUrl ->
                if (rootNavController.popBackStack()) {
                    rootNavController.currentBackStackEntry?.savedStateHandle?.let { handle ->
                        handle["navigate_to_pending_chat_id"] = partnerId
                        handle["navigate_to_pending_chat_name"] = displayName
                        handle["navigate_to_pending_chat_avatar"] = avatarUrl
                    }
                }
            },
            onNavigateToEditProfile = {
                rootNavController.navigate(EditProfile)
            }
        )
    }

    composable<CreateGroup> {
        CreateGroupScreen(
            onBack = { rootNavController.popBackStack() },
            onNavigateToConversation = { conversationId ->
                if (rootNavController.popBackStack()) {
                    rootNavController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("navigate_to_chat", conversationId)
                }
            }
        )
    }
}
