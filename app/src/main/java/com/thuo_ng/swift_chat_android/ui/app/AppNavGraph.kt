package com.thuo_ng.swift_chat_android.ui.app

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thuo_ng.swift_chat_android.ui.auth.authGraph
import com.thuo_ng.swift_chat_android.ui.main.mainGraph
import com.thuo_ng.swift_chat_android.ui.navigation.AuthGraph
import com.thuo_ng.swift_chat_android.ui.navigation.MainGraph
import kotlinx.serialization.Serializable



@Serializable data object RootGraph
@Serializable data object SplashRoute
@Composable
fun AppNavGraph(
    appViewModel: AppViewModel = hiltViewModel()
) {
    val authState by appViewModel.authState.collectAsStateWithLifecycle()
    val rootNavController = rememberNavController()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        appViewModel.effect.collect { effect ->
            when (effect) {
                is AppEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    if (authState == AppAuthState.Loading) return

    NavHost(
        navController = rootNavController,
        startDestination = SplashRoute,
        route = RootGraph::class,
        modifier = Modifier.fillMaxSize()
    ) {
        composable<SplashRoute> {}
        authGraph(rootNavController = rootNavController)
        mainGraph(rootNavController = rootNavController)
    }

    LaunchedEffect(authState) {
        when (authState) {
            AppAuthState.Loading -> Unit
            AppAuthState.Authenticated -> rootNavController.navigate(MainGraph) {
                popUpTo<RootGraph> { inclusive = true }
                launchSingleTop = true
            }
            AppAuthState.Unauthenticated -> rootNavController.navigate(AuthGraph) {
                popUpTo<RootGraph> { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}
