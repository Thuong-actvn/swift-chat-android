package com.thuo_ng.swift_chat_android.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenRequest(
    val token: String,
    val platform: String = "android"
)
