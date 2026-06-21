package com.thuo_ng.swift_chat_android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.thuo_ng.swift_chat_android.R

 val InterFontFamily = FontFamily(
     Font(R.font.inter_regular, FontWeight.Normal),
     Font(R.font.inter_medium,  FontWeight.Medium),
     Font(R.font.inter_semibold, FontWeight.SemiBold),
     Font(R.font.inter_bold,    FontWeight.Bold),
 )

//private val InterFontFamily = FontFamily.Default

val SwiftChatTypography = Typography(

    // design spec: display-lg
    displayLarge = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 32.sp,
        lineHeight    = 40.sp,
        letterSpacing = (-0.64).sp,   // -0.02em × 32sp
    ),

    // design spec: headline-md
    headlineMedium = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 22.sp,
        lineHeight    = 28.sp,
        letterSpacing = (-0.22).sp,   // -0.01em × 22sp
    ),

    // design spec: headline-md-mobile (mapped to headlineSmall)
    headlineSmall = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 20.sp,
        lineHeight    = 26.sp,
        letterSpacing = 0.sp,
    ),

    // design spec: title-lg
    titleLarge = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 18.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.sp,
    ),

    // design spec: body-large
    bodyLarge = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.sp,
    ),

    // design spec: body-medium
    bodyMedium = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.sp,
    ),

    // design spec: label-caps
    labelSmall = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.6.sp,   // 0.05em × 12sp
    ),
)