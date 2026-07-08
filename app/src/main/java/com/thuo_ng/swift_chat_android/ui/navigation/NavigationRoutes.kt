package com.thuo_ng.swift_chat_android.ui.navigation

import kotlinx.serialization.Serializable

// ── Root graphs ──────────────────────────────────────────
@Serializable object AuthGraph
@Serializable object MainGraph

// ── Auth routes ──────────────────────────────────────────
@Serializable object SignIn
@Serializable object Signup

// ── Main (tab) routes ────────────────────────────────────
@Serializable object Conversations
@Serializable data class ChatDetail(val conversationId: String)
@Serializable data class PendingDirectChat(
    val partnerId: String,
    val displayName: String,
    val avatarUrl: String? = null
)

@Serializable object Friends
@Serializable object Notifications
@Serializable object Profile

// ── Root routes (Full screen) ────────────────────────────
@Serializable object EditProfile
