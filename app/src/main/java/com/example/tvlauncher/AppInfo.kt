package com.example.tvlauncher

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable,
    val pinned: Boolean = false,
    val usage: Int = 0,
    val lastUsed: Long = 0L
)