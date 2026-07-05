package com.thuo_ng.swift_chat_android.ui.main

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.thuo_ng.swift_chat_android.ui.navigation.AuthGraph
import com.thuo_ng.swift_chat_android.ui.navigation.EditProfile
import com.thuo_ng.swift_chat_android.ui.navigation.MainGraph
import com.thuo_ng.swift_chat_android.ui.profile.edit.EditProfileScreen


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
}
