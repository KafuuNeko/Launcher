package me.kafuuneko.launcher.feature.main.presentation

sealed class MainUiIntent {
    data object Init : MainUiIntent()

    data object Resume : MainUiIntent()

    data object RefreshApps : MainUiIntent()

    data class AppClick(val packageName: String) : MainUiIntent()

    data class AppLongClick(val packageName: String) : MainUiIntent()

    data object OpenSettings : MainUiIntent()

    data object ChangeWallpaper : MainUiIntent()

    // 页面切换
    data class NavigateToPage(val page: PageType) : MainUiIntent()

    data object GoBack : MainUiIntent()

    // 搜索
    data class SearchQueryChange(val query: String) : MainUiIntent()

    // 手势操作
    data object SwipeLeft : MainUiIntent()

    data object SwipeRight : MainUiIntent()

    data object SwipeDown : MainUiIntent()

    data object SwipeUp : MainUiIntent()
}
