package me.kafuuneko.launcher.feature.main.presentation

import android.content.Intent
import me.kafuuneko.launcher.libs.core.IViewEvent

sealed class MainViewEvent : IViewEvent {
    data class StartApp(val intent: Intent) : MainViewEvent()

    data class ShowAppInfo(val packageName: String) : MainViewEvent()

    data object OpenWallpaperPicker : MainViewEvent()
}
