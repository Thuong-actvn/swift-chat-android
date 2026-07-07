package com.thuo_ng.swift_chat_android

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.thuo_ng.swift_chat_android.ui.app.AppNavGraph
import com.thuo_ng.swift_chat_android.ui.theme.SwiftChatTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.thuo_ng.swift_chat_android.ui.app.AppAuthState
import com.thuo_ng.swift_chat_android.ui.app.AppViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel : AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition {
            viewModel.authState.value == AppAuthState.Loading
        }
        enableEdgeToEdge()
        @Suppress("DEPRECATION")
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContent {
            SwiftChatTheme {
                AppNavGraph(viewModel)
            }
        }
    }
}
