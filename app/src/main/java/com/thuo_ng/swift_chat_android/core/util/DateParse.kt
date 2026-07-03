package com.thuo_ng.swift_chat_android.core.util

import java.time.Instant

fun String.toEpochMilli(): Long =
    Instant.parse(this).toEpochMilli()