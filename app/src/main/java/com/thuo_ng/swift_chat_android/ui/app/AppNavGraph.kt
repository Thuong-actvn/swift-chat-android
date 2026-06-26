package com.thuo_ng.swift_chat_android.ui.app

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.thuo_ng.swift_chat_android.core.session.SessionEvent
import com.thuo_ng.swift_chat_android.ui.auth.authGraph
import com.thuo_ng.swift_chat_android.ui.main.mainGraph
import com.thuo_ng.swift_chat_android.ui.navigation.AuthGraph
import com.thuo_ng.swift_chat_android.ui.navigation.MainGraph

/**
 * Root navigation shell: AuthGraph ↔ MainGraph.
 *
 *   rootNavController
 *   ├── AuthGraph (SignIn, Signup)
 *   └── MainGraph → MainScreen (tab NavHost)
 */
@Composable
fun AppNavGraph(
    appViewModel: AppViewModel = hiltViewModel()
) {
    val rootNavController = rememberNavController()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        appViewModel.sessionEvent.collect { event ->
            when (event) {
                is SessionEvent.Expired -> {
                    Toast.makeText(
                        context,
                        "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại",
                        Toast.LENGTH_LONG
                    ).show()
                    rootNavController.navigate(AuthGraph) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                is SessionEvent.LoggedOut -> {
                    rootNavController.navigate(AuthGraph) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    NavHost(
        navController = rootNavController,
        startDestination = if (appViewModel.isLoggedIn.collectAsState().value) MainGraph else AuthGraph,
        modifier = Modifier.fillMaxSize()
    ) {
        authGraph(rootNavController = rootNavController)
        mainGraph(rootNavController = rootNavController)
    }
}
