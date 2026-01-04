package me.kafuuneko.launcher.feature.main.presentation

sealed class MainUiIntent {
    data object Init : MainUiIntent()

    data object Resume : MainUiIntent()

    data object RefreshApps : MainUiIntent()

    data class AppClick(val packageName: String) : MainUiIntent()

    data class AppLongClick(val packageName: String) : MainUiIntent()

    data object OpenSettings : MainUiIntent()

    data object ChangeWallpaper : MainUiIntent()
}
