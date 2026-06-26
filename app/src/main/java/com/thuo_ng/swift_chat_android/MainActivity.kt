package com.thuo_ng.swift_chat_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.thuo_ng.swift_chat_android.ui.app.AppNavGraph
import com.thuo_ng.swift_chat_android.ui.theme.SwiftChatTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwiftChatTheme {
                AppNavGraph()
            }
        }
    }
}