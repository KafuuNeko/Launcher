package me.kafuuneko.launcher.feature.main.ui.pages

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.kafuuneko.launcher.feature.main.presentation.MainUiIntent
import me.kafuuneko.launcher.feature.main.presentation.MainUiState
import me.kafuuneko.launcher.libs.model.AppInfo
import java.time.format.DateTimeFormatter

/**
 * 主页组件
 * 包含：时间显示、搜索框、最近使用的应用、功能入口
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomePage(
    uiState: MainUiState.Normal,
    emitIntent: (MainUiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // 获取屏幕高度
    val screenHeight = with(density) {
        androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    // 跟踪向上拖动的距离
    var upwardDragOffset by remember { mutableFloatStateOf(0f) }
    // 是否已经触发导航
    var navigationTriggered by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 时间显示
            item {
                TimeDisplay(uiState)
            }

            // 搜索框
            item {
                SearchBox(uiState, emitIntent)
            }

            // 最近使用的应用标题
            if (uiState.recentApps.isNotEmpty()) {
                item {
                    Text(
                        text = "最近使用",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 最近使用的应用列表 - 横向滚动
                item {
                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.recentApps.forEach { app ->
                            RecentAppItem(
                                app = app,
                                onClick = { emitIntent(MainUiIntent.AppClick(app.packageName)) },
                                onLongClick = { emitIntent(MainUiIntent.AppLongClick(app.packageName)) }
                            )
                        }
                    }
                }
            }

            // 功能入口
            item {
                QuickActions(emitIntent)
            }

            // 底部占位
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // 透明的手势检测层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            // 不 consume 事件，让 LazyColumn 也能滚动

                            // 只响应向上的拖动（dragAmount < 0）
                            if (dragAmount < 0 && !navigationTriggered) {
                                upwardDragOffset += -dragAmount

                                // 如果拖动超过阈值，触发打开应用列表
                                if (upwardDragOffset > screenHeight * 0.2f) {
                                    navigationTriggered = true
                                    emitIntent(
                                        MainUiIntent.NavigateToPage(
                                            me.kafuuneko.launcher.feature.main.presentation.PageType.ALL_APPS
                                        )
                                    )
                                }
                            } else if (dragAmount > 0) {
                                // 向下拖动时，减少偏移量
                                upwardDragOffset = maxOf(0f, upwardDragOffset - dragAmount)
                            }
                        },
                        onDragEnd = {
                            // 拖动结束时重置偏移量和触发标志
                            upwardDragOffset = 0f
                            navigationTriggered = false
                        },
                        onDragCancel = {
                            upwardDragOffset = 0f
                            navigationTriggered = false
                        }
                    )
                }
        )
    }
}

/**
 * 时间显示组件
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TimeDisplay(uiState: MainUiState.Normal) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE")

        Text(
            text = uiState.currentTime.format(timeFormatter),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = uiState.currentTime.format(dateFormatter),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

/**
 * 搜索框组件
 */
@Composable
private fun SearchBox(
    uiState: MainUiState.Normal,
    emitIntent: (MainUiIntent) -> Unit
) {
    OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = { query ->
            emitIntent(MainUiIntent.SearchQueryChange(query))
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        placeholder = {
            Text("搜索应用...")
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "搜索")
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                // 搜索逻辑自动触发，无需额外处理
            }
        ),
        shape = RoundedCornerShape(24.dp),
        textStyle = MaterialTheme.typography.bodyLarge
    )
}


/**
 * 最近应用项 - 横向滚动卡片
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentAppItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 应用图标
            androidx.compose.foundation.Image(
                painter = coil.compose.rememberAsyncImagePainter(app.icon),
                contentDescription = app.name.toString(),
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 应用名称
            Text(
                text = app.name.toString(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

/**
 * 快捷功能入口
 */
@Composable
private fun QuickActions(emitIntent: (MainUiIntent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "快捷功能",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            QuickActionCard(
                title = "信息",
                icon = null,
                onClick = {
                    emitIntent(MainUiIntent.NavigateToPage(me.kafuuneko.launcher.feature.main.presentation.PageType.INFO))
                },
                modifier = Modifier.weight(1f)
            )

            QuickActionCard(
                title = "所有应用",
                icon = null,
                onClick = {
                    emitIntent(MainUiIntent.NavigateToPage(me.kafuuneko.launcher.feature.main.presentation.PageType.ALL_APPS))
                },
                modifier = Modifier.weight(1f)
            )

            QuickActionCard(
                title = "更多",
                icon = null,
                onClick = {
                    emitIntent(MainUiIntent.NavigateToPage(me.kafuuneko.launcher.feature.main.presentation.PageType.MORE))
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 快捷功能卡片
 */
@Composable
private fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
