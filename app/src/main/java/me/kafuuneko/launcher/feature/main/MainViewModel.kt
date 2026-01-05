package me.kafuuneko.launcher.feature.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.kafuuneko.launcher.feature.main.presentation.MainUiIntent
import me.kafuuneko.launcher.feature.main.presentation.MainUiState
import me.kafuuneko.launcher.feature.main.presentation.MainViewEvent
import me.kafuuneko.launcher.feature.main.presentation.PageType
import me.kafuuneko.launcher.libs.AppLibs
import me.kafuuneko.launcher.libs.core.AppViewEvent
import me.kafuuneko.launcher.libs.core.CoreViewModelWithEvent
import me.kafuuneko.launcher.libs.core.UiIntentObserver
import me.kafuuneko.launcher.libs.model.AppInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainViewModel : CoreViewModelWithEvent<MainUiIntent, MainUiState>(
    initStatus = MainUiState.Loading
), KoinComponent {
    private val mContext by inject<Context>()
    private val mAppLibs by inject<AppLibs>()

    // 最近使用的应用缓存（使用LinkedHashSet保持插入顺序）
    private val recentAppsCache = LinkedHashSet<String>()
    private val MAX_RECENT_APPS = 8

    /**
     * 获取已安装的应用列表
     */
    @SuppressLint("QueryPermissionsNeeded")
    private fun getInstalledApps(): List<AppInfo> {
        val packageManager = mContext.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        return installedApps
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { appInfo ->
                AppInfo(
                    packageName = appInfo.packageName,
                    name = appInfo.loadLabel(packageManager).toString(),
                    icon = try {
                        appInfo.loadIcon(packageManager)
                    } catch (e: Exception) {
                        null
                    },
                    applicationInfo = appInfo
                )
            }
            .sortedBy { it.name.toString() }
    }

    /**
     * 过滤应用列表
     */
    private fun filterApps(query: String, apps: List<AppInfo>): List<AppInfo> {
        return if (query.isEmpty()) {
            apps
        } else {
            apps.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    /**
     * 获取最近使用的应用
     */
    private fun getRecentApps(allApps: List<AppInfo>): List<AppInfo> {
        return allApps.filter { it.packageName in recentAppsCache }
            .sortedByDescending { recentAppsCache.contains(it.packageName) }
            .take(MAX_RECENT_APPS)
    }

    /**
     * 添加到最近使用
     */
    private fun addToRecent(packageName: String) {
        recentAppsCache.remove(packageName)
        recentAppsCache.add(packageName)
        if (recentAppsCache.size > MAX_RECENT_APPS) {
            // 移除最早添加的元素
            val first = recentAppsCache.first()
            recentAppsCache.remove(first)
        }
    }

    /**
     * 页面初始化
     */
    @UiIntentObserver(MainUiIntent.Init::class)
    private suspend fun onInit() {
        val apps = getInstalledApps()
        MainUiState.Normal(
            apps = apps,
            filteredApps = apps,
            recentApps = getRecentApps(apps)
        ).setup()
        startTimeUpdate()
    }

    /**
     * 启动时间更新
     */
    private suspend fun startTimeUpdate() = viewModelScope.launch{
        while (isActive) {
            delay(1000)
            getOrNull<MainUiState.Normal>()?.let { state ->
                state.copy(currentTime = LocalDateTime.now()).setup()
            }
        }
    }

    /**
     * 页面恢复
     */
    @UiIntentObserver(MainUiIntent.Resume::class)
    private suspend fun onResume() {
        getOrNull<MainUiState.Normal>()?.let { state ->
            val apps = getInstalledApps()
            state.copy(
                apps = apps,
                filteredApps = filterApps(state.searchQuery, apps),
                recentApps = getRecentApps(apps)
            ).setup()
        }
    }

    /**
     * 刷新应用列表
     */
    @UiIntentObserver(MainUiIntent.RefreshApps::class)
    private suspend fun onRefreshApps() {
        getOrNull<MainUiState.Normal>()?.let { state ->
            val apps = getInstalledApps()
            state.copy(
                apps = apps,
                filteredApps = filterApps(state.searchQuery, apps),
                recentApps = getRecentApps(apps)
            ).setup()
        }
    }

    /**
     * 应用点击
     */
    @UiIntentObserver(MainUiIntent.AppClick::class)
    private suspend fun onAppClick(intent: MainUiIntent.AppClick) {
        addToRecent(intent.packageName)
        val packageManager = mContext.packageManager
        try {
            val appIntent = packageManager.getLaunchIntentForPackage(intent.packageName)
            appIntent?.let {
                MainViewEvent.StartApp(it).emit()
            }
        } catch (e: Exception) {
            AppViewEvent.PopupToastMessage("无法启动应用").emit()
        }
    }

    /**
     * 应用长按
     */
    @UiIntentObserver(MainUiIntent.AppLongClick::class)
    private suspend fun onAppLongClick(intent: MainUiIntent.AppLongClick) {
        MainViewEvent.ShowAppInfo(intent.packageName).emit()
    }

    /**
     * 打开设置
     */
    @UiIntentObserver(MainUiIntent.OpenSettings::class)
    private suspend fun onOpenSettings() {
        try {
            val settingsIntent =
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${mContext.packageName}".toUri()
                }
            MainViewEvent.StartApp(settingsIntent).emit()
        } catch (e: Exception) {
            AppViewEvent.PopupToastMessage("无法打开设置").emit()
        }
    }

    /**
     * 更换壁纸
     */
    @UiIntentObserver(MainUiIntent.ChangeWallpaper::class)
    private suspend fun onChangeWallpaper() {
        MainViewEvent.OpenWallpaperPicker.emit()
    }

    /**
     * 页面导航
     */
    @UiIntentObserver(MainUiIntent.NavigateToPage::class)
    private suspend fun onNavigateToPage(intent: MainUiIntent.NavigateToPage) {
        getOrNull<MainUiState.Normal>()?.copy(currentPage = intent.page)?.setup()
    }

    /**
     * 返回上一页
     */
    @UiIntentObserver(MainUiIntent.GoBack::class)
    private suspend fun onGoBack() {
        getOrNull<MainUiState.Normal>()?.let { state ->
            when (state.currentPage) {
                PageType.ALL_APPS -> state.copy(currentPage = PageType.HOME).setup()
                PageType.INFO, PageType.MORE -> state.copy(currentPage = PageType.HOME).setup()
                PageType.HOME -> Unit // 主页不处理返回
            }
        }
    }

    /**
     * 搜索查询变化
     */
    @UiIntentObserver(MainUiIntent.SearchQueryChange::class)
    private suspend fun onSearchQueryChange(intent: MainUiIntent.SearchQueryChange) {
        getOrNull<MainUiState.Normal>()?.let { state ->
            state.copy(
                searchQuery = intent.query,
                filteredApps = filterApps(intent.query, state.apps)
            ).setup()
        }
    }

    /**
     * 左滑（到信息页面）
     */
    @UiIntentObserver(MainUiIntent.SwipeLeft::class)
    private suspend fun onSwipeLeft() {
        getOrNull<MainUiState.Normal>()?.let { state ->
            when (state.currentPage) {
                PageType.HOME -> state.copy(currentPage = PageType.INFO).setup()
                else -> Unit
            }
        }
    }

    /**
     * 右滑（到更多功能页面）
     */
    @UiIntentObserver(MainUiIntent.SwipeRight::class)
    private suspend fun onSwipeRight() {
        getOrNull<MainUiState.Normal>()?.let { state ->
            when (state.currentPage) {
                PageType.HOME -> state.copy(currentPage = PageType.MORE).setup()
                else -> Unit
            }
        }
    }

    /**
     * 下滑（到所有应用页面）
     */
    @UiIntentObserver(MainUiIntent.SwipeDown::class)
    private suspend fun onSwipeDown() {
        getOrNull<MainUiState.Normal>()?.let { state ->
            when (state.currentPage) {
                PageType.HOME -> state.copy(currentPage = PageType.ALL_APPS).setup()
                else -> Unit
            }
        }
    }

    /**
     * 上滑（返回主页）
     */
    @UiIntentObserver(MainUiIntent.SwipeUp::class)
    private suspend fun onSwipeUp() {
        getOrNull<MainUiState.Normal>()?.let { state ->
            when (state.currentPage) {
                PageType.ALL_APPS -> state.copy(currentPage = PageType.HOME).setup()
                else -> Unit
            }
        }
    }
}
