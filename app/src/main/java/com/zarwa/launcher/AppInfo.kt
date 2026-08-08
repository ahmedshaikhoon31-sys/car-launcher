package com.zarwa.launcher

import android.graphics.drawable.Drawable

/** One installed, launchable application. */
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable
)
