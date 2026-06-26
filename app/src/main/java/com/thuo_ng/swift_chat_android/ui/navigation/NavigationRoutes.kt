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

@Serializable object Calls
@Serializable object Friends
@Serializable object Profile
