package com.thuo_ng.swift_chat_android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val SwiftChatShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // asymmetric bubble point
    small      = RoundedCornerShape(8.dp),   // small cards
    medium     = RoundedCornerShape(12.dp),  // medium cards
    large      = RoundedCornerShape(16.dp),  // buttons, text fields
    extraLarge = RoundedCornerShape(24.dp),  // FAB (morphed square)
)

// Chat Bubble Shapes (asymmetric corners)

/** Sent bubble: bottom-right corner is flattened (4dp) */
val BubbleSentShape = RoundedCornerShape(
    topStart    = 20.dp,
    topEnd      = 20.dp,
    bottomStart = 20.dp,
    bottomEnd   = 4.dp,
)

/** Received bubble: bottom-left corner is flattened (4dp) */
val BubbleReceivedShape = RoundedCornerShape(
    topStart    = 20.dp,
    topEnd      = 20.dp,
    bottomStart = 4.dp,
    bottomEnd   = 20.dp,
)

/** Pill shape for chat input field */
val InputFieldShape = RoundedCornerShape(28.dp)
