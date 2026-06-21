package com.thuo_ng.swift_chat_android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color


@Immutable
data class ChatColor(

    // Chat Bubbles
    val bubbleSent: Color,
    val onBubbleSent: Color,
    val bubbleReceived: Color,
    val onBubbleReceived: Color,

    // Presence
    val onlineIndicator: Color,

    // Chat Input Field
    val inputBackground: Color,
    val inputBorder: Color,
)

// Light values
val LightChatColor = ChatColor(
    bubbleSent       = Blue50,          // #1CA0E1 — primary-container
    onBubbleSent     = Neutral100,      // white text
    bubbleReceived   = NeutralVariant90, // surface-variant
    onBubbleReceived = Neutral10,       // on-surface

    onlineIndicator  = Green90,         // #91F78E — secondary-container

    inputBackground  = Neutral94,       // surface-container
    inputBorder      = NeutralVariant80, // outline-variant
)

// CompositionLocal
val LocalChatColor = staticCompositionLocalOf { LightChatColor }
