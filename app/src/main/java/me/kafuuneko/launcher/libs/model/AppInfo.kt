package me.kafuuneko.launcher.libs.model

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val name: CharSequence,
    val icon: Drawable?,
    val applicationInfo: ApplicationInfo
)
