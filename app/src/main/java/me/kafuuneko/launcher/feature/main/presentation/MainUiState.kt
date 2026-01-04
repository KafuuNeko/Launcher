package me.kafuuneko.launcher.feature.main.presentation

import androidx.compose.foundation.lazy.LazyListState
import me.kafuuneko.launcher.libs.model.AppInfo

sealed class MainUiState {
    data object Loading : MainUiState()

    data class Normal(
        val apps: List<AppInfo> = emptyList(),
        val searchQuery: String = "",
        val filteredApps: List<AppInfo> = emptyList(),
        val listState: LazyListState = LazyListState()
    ) : MainUiState()
}
