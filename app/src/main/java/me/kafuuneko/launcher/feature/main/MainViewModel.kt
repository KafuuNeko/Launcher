package me.kafuuneko.launcher.feature.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.net.toUri
import me.kafuuneko.launcher.feature.main.presentation.MainUiIntent
import me.kafuuneko.launcher.feature.main.presentation.MainUiState
import me.kafuuneko.launcher.feature.main.presentation.MainViewEvent
import me.kafuuneko.launcher.libs.AppLibs
import me.kafuuneko.launcher.libs.core.AppViewEvent
import me.kafuuneko.launcher.libs.core.CoreViewModelWithEvent
import me.kafuuneko.launcher.libs.core.UiIntentObserver
import me.kafuuneko.launcher.libs.model.AppInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainViewModel : CoreViewModelWithEvent<MainUiIntent, MainUiState>(
    initStatus = MainUiState.Loading
), KoinComponent {
    private val mContext by inject<Context>()
    private val mAppLibs by inject<AppLibs>()

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
     * 页面初始化
     */
    @UiIntentObserver(MainUiIntent.Init::class)
    private suspend fun onInit() {
        val apps = getInstalledApps()
        MainUiState.Normal(
            apps = apps,
            filteredApps = apps
        ).setup()
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
                filteredApps = filterApps(state.searchQuery, apps)
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
                filteredApps = filterApps(state.searchQuery, apps)
            ).setup()
        }
    }

    /**
     * 应用点击
     */
    @UiIntentObserver(MainUiIntent.AppClick::class)
    private suspend fun onAppClick(intent: MainUiIntent.AppClick) {
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
}
