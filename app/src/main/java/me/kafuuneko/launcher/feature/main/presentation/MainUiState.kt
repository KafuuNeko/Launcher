package me.kafuuneko.launcher.feature.main.presentation

import androidx.compose.foundation.lazy.LazyListState
import me.kafuuneko.launcher.libs.model.AppInfo
import java.time.LocalDateTime

/**
 * 页面类型枚举
 */
enum class PageType {
    HOME,           // 主页
    INFO,           // 信息页面
    MORE,           // 更多功能页面
    ALL_APPS        // 所有应用页面
}

sealed class MainUiState {
    data object Loading : MainUiState()

    data class Normal(
        val currentPage: PageType = PageType.HOME,
        val apps: List<AppInfo> = emptyList(),
        val recentApps: List<AppInfo> = emptyList(),
        val searchQuery: String = "",
        val filteredApps: List<AppInfo> = emptyList(),
        val listState: LazyListState = LazyListState(),
        val currentTime: LocalDateTime = LocalDateTime.now()
    ) : MainUiState()
}
