package me.kafuuneko.launcher.feature.main.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.kafuuneko.launcher.feature.main.presentation.MainUiIntent
import me.kafuuneko.launcher.feature.main.presentation.MainUiState
import me.kafuuneko.launcher.feature.main.presentation.PageType
import me.kafuuneko.launcher.feature.main.ui.pages.AllAppsPage
import me.kafuuneko.launcher.feature.main.ui.pages.HomePage
import me.kafuuneko.launcher.feature.main.ui.pages.InfoPage
import me.kafuuneko.launcher.feature.main.ui.pages.MorePage

@Composable
fun MainLayout(
    uiState: MainUiState,
    emitIntent: (MainUiIntent) -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        when (uiState) {
            is MainUiState.Loading -> LoadingView()
            is MainUiState.Normal -> NormalView(uiState, emitIntent)
        }
    }
}

/**
 * 加载视图
 */
@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "正在加载应用...",
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 正常视图（带手势导航和页面切换）
 */
@Composable
private fun NormalView(
    uiState: MainUiState.Normal,
    emitIntent: (MainUiIntent) -> Unit
) {
    if (uiState.currentPage == PageType.ALL_APPS) {
        // 显示所有应用页面
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            AllAppsPage(
                uiState = uiState,
                emitIntent = emitIntent
            )
        }
    } else {
        // 使用 HorizontalPager 显示 INFO、HOME、MORE
        HorizontalPagerView(
            uiState = uiState,
            emitIntent = emitIntent
        )
    }
}

/**
 * 使用 HorizontalPager 的水平滑动视图
 * 包含 INFO(0)、HOME(1)、MORE(2) 三个页面
 */
@Composable
private fun HorizontalPagerView(
    uiState: MainUiState.Normal,
    emitIntent: (MainUiIntent) -> Unit
) {
    // 将 PageType 映射到 Pager 页面索引
    // INFO -> 0, HOME -> 1, MORE -> 2
    val initialPage = when (uiState.currentPage) {
        PageType.INFO -> 0
        PageType.HOME -> 1
        PageType.MORE -> 2
        else -> 1
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 3 }
    )

    // 监听 pager 页面变化，同步到 uiState
    // 使用 key 来避免重复触发
    LaunchedEffect(pagerState.currentPage) {
        val pageType = when (pagerState.currentPage) {
            0 -> PageType.INFO
            1 -> PageType.HOME
            2 -> PageType.MORE
            else -> PageType.HOME
        }
        if (uiState.currentPage != pageType) {
            emitIntent(MainUiIntent.NavigateToPage(pageType))
        }
    }

    LaunchedEffect(uiState.currentPage) {
        val pageIndex = when (uiState.currentPage) {
            PageType.INFO -> 0
            PageType.HOME -> 1
            PageType.MORE -> 2
            PageType.ALL_APPS -> return@LaunchedEffect
        }
        if (pageIndex != pagerState.currentPage) {
            pagerState.scrollToPage(pageIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> InfoPage(emitIntent)
                1 -> HomePage(uiState, emitIntent)
                2 -> MorePage(emitIntent)
            }
        }
    }
}
