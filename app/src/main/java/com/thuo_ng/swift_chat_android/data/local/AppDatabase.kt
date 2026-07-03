package com.thuo_ng.swift_chat_android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.thuo_ng.swift_chat_android.data.local.dao.UserDao
import com.thuo_ng.swift_chat_android.data.local.entity.UserEntity


@Database(entities = [UserEntity::class], exportSchema = false, version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun userDao(): UserDao
}