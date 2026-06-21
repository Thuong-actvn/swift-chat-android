package com.thuo_ng.swift_chat_android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val LightColorScheme = lightColorScheme(
    primary              = Blue40,
    onPrimary            = Neutral100,
    primaryContainer     = Blue50,
    onPrimaryContainer   = Blue10,
    inversePrimary       = Blue80,

    secondary            = Green30,
    onSecondary          = Neutral100,
    secondaryContainer   = Green90,
    onSecondaryContainer = Green40,

    tertiary             = Amber40,
    onTertiary           = Neutral100,
    tertiaryContainer    = Amber50,
    onTertiaryContainer  = Amber20,

    error                = Red40,
    onError              = Neutral100,
    errorContainer       = Red90,
    onErrorContainer     = Red10,

    background           = Neutral99,
    onBackground         = Neutral10,

    surface              = Neutral99,
    onSurface            = Neutral10,
    surfaceVariant       = NeutralVariant90,
    onSurfaceVariant     = NeutralVariant30,
    surfaceTint          = Blue40,

    surfaceBright              = Neutral99,
    surfaceDim                 = Neutral87,
    surfaceContainer           = Neutral94,
    surfaceContainerHigh       = Neutral92,
    surfaceContainerHighest    = Neutral90,
    surfaceContainerLow        = Neutral96,
    surfaceContainerLowest     = Neutral100,

    inverseSurface       = Neutral20,
    inverseOnSurface     = NeutralVariant95,

    outline              = NeutralVariant50,
    outlineVariant       = NeutralVariant80,

    scrim                = Neutral0,
)

// Theme Composable

@Composable
fun SwiftChatTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalChatColor provides LightChatColor,
    ) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography  = SwiftChatTypography,
            shapes      = SwiftChatShapes,
            content     = content,
        )
    }
}

// Convenience Accessor

object SwiftChatThemeTokens {
    /** Chat-specific color tokens not covered by MaterialTheme.colorScheme. */
    val chatColors: ChatColor
        @Composable
        @ReadOnlyComposable
        get() = LocalChatColor.current
}