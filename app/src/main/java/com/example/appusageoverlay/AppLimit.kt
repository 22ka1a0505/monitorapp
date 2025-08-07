package com.example.appusageoverlay

data class AppLimit(
    val packageName: String,
    val appName: String,
    val timeLimitMinutes: Int,
    var usedMinutesToday: Int = 0
)
