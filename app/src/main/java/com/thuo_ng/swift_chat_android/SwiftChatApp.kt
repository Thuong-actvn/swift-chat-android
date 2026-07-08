package com.thuo_ng.swift_chat_android

import android.app.Application
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class SwiftChatApp : Application() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SwiftChatAppEntryPoint {
        fun secureStorage(): SecureStorage
    }

    override fun onCreate() {
        super.onCreate()
        refreshFcmTokenIfNeeded()
    }

    private fun refreshFcmTokenIfNeeded() {
        val secureStorage = EntryPoints.get(this, SwiftChatAppEntryPoint::class.java).secureStorage()
        if (secureStorage.getFcmToken() == null) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                secureStorage.saveFcmToken(token)
                Log.d("SwiftChatApp", "FCM token restored to storage")
            }
        }
    }
}
