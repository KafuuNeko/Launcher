package me.kafuuneko.launcher.feature.main.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import me.kafuuneko.launcher.feature.main.presentation.MainUiIntent
import me.kafuuneko.launcher.feature.main.presentation.MainUiState
import me.kafuuneko.launcher.feature.main.presentation.PageType
import me.kafuuneko.launcher.feature.main.ui.pages.AllAppsPage
import me.kafuuneko.launcher.feature.main.ui.pages.HomePage
import me.kafuuneko.launcher.feature.main.ui.pages.InfoPage
import me.kafuuneko.launcher.feature.main.ui.pages.MorePage
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
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
 * 支持底部抽屉式应用列表
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun NormalView(
    uiState: MainUiState.Normal,
    emitIntent: (MainUiIntent) -> Unit
) {
    val isAllAppsVisible = uiState.currentPage == PageType.ALL_APPS

    // 使用 Box 叠加布局
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 背景：HorizontalPager (INFO、HOME、MORE)
        HorizontalPagerView(
            uiState = uiState,
            emitIntent = emitIntent,
            modifier = Modifier.fillMaxSize()
        )

        // 前景：应用列表抽屉
        AllAppsDrawerSheet(
            isVisible = isAllAppsVisible,
            uiState = uiState,
            emitIntent = emitIntent,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * 使用 HorizontalPager 的水平滑动视图
 * 包含 INFO(0)、HOME(1)、MORE(2) 三个页面
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun HorizontalPagerView(
    uiState: MainUiState.Normal,
    emitIntent: (MainUiIntent) -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier
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

/**
 * 应用列表抽屉
 * 支持从底部滑入/滑出动画
 * 支持跟随手指拖动
 */
@Composable
private fun AllAppsDrawerSheet(
    isVisible: Boolean,
    uiState: MainUiState.Normal,
    emitIntent: (MainUiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // 获取屏幕高度
    val screenHeight = with(density) {
        androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    // 抽屉偏移量（当前实际位置）
    var currentOffset by remember { mutableFloatStateOf(if (isVisible) 0f else screenHeight) }

    // 是否正在拖动
    var isDragging by remember { mutableStateOf(false) }

    // 动画目标
    val targetOffset = if (isVisible) 0f else screenHeight

    // 只有在不拖动时才使用动画
    val animatedOffset by animateFloatAsState(
        targetValue = if (!isDragging) targetOffset else currentOffset,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 300f
        ),
        label = "drawer_offset"
    )

    // 当可见状态改变时，重置偏移量
    LaunchedEffect(isVisible) {
        if (!isDragging) {
            currentOffset = targetOffset
        }
    }

    Box(
        modifier = modifier
            .offset { IntOffset(0, animatedOffset.roundToInt()) }
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AllAppsPage(
            uiState = uiState,
            emitIntent = emitIntent,
            onTopBarDrag = { dragAmount ->
                // 向下拖动顶部工具栏
                if (!isDragging) {
                    isDragging = true
                }
                currentOffset += dragAmount

                // 限制偏移量范围
                currentOffset = currentOffset.coerceIn(0f, screenHeight)
            },
            onTopBarDragEnd = {
                isDragging = false

                // 拖动结束，判断应该展开还是关闭
                val shouldClose = currentOffset > screenHeight * 0.3f
                if (shouldClose) {
                    emitIntent(MainUiIntent.GoBack)
                } else {
                    // 弹回展开状态
                    currentOffset = 0f
                }
            }
        )
    }
}
