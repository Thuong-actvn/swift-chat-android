package com.thuo_ng.swift_chat_android.ui.main

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.thuo_ng.swift_chat_android.ui.navigation.AuthGraph
import com.thuo_ng.swift_chat_android.ui.navigation.MainGraph

/**
 * MainGraph chứa MainScreen — Scaffold với BottomBar + tab NavHost riêng.
 *
 * MainScreen có mainNavController riêng, hoàn toàn tách biệt rootNavController.
 * Khi cần logout, dùng AppViewModel observe sessionEvent → pop về AuthGraph.
 */
fun NavGraphBuilder.mainGraph(rootNavController: NavController) {
    composable<MainGraph> {
        MainScreen(
            onLogout = {
                rootNavController.navigate(AuthGraph) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }
}
