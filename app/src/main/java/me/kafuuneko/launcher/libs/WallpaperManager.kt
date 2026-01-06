package me.kafuuneko.launcher.libs

import android.app.WallpaperManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * 系统壁纸管理器
 * 用于获取和监听系统壁纸变化
 */
object WallpaperHelper {

    private const val TAG = "WallpaperHelper"

    /**
     * 获取当前系统壁纸
     * 注意：虽然某些 Android 版本理论上不需要权限，
     * 但部分设备制造商可能有自己的限制，所以仍可能需要处理 SecurityException
     */
    fun getWallpaper(context: Context): Drawable? {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            Log.d(TAG, "API Level: ${android.os.Build.VERSION.SDK_INT}")

            // 对于所有 Android 版本，使用相同的方法获取壁纸
            // getDrawable() 虽然在 Android 13+ 被标记为弃用，但仍然可用
            @Suppress("DEPRECATION")
            val wallpaper = wallpaperManager.drawable

            if (wallpaper != null) {
                Log.d(TAG, "Wallpaper retrieved: ${wallpaper.javaClass}")
                Log.d(TAG, "Wallpaper size: ${wallpaper.intrinsicWidth}x${wallpaper.intrinsicHeight}")
            } else {
                Log.w(TAG, "Wallpaper is null - device may not have a wallpaper set")
            }

            return wallpaper
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}", e)
            Log.e(TAG, "Device may require READ_EXTERNAL_STORAGE permission despite Android docs")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get wallpaper: ${e.message}", e)
            null
        }
    }
}

/**
 * Composable 函数，用于获取并记住系统壁纸
 *
 * @return 当前系统壁纸的 Drawable，如果获取失败则返回 null
 */
@Composable
fun rememberWallpaper(): Drawable? {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    // 仅在预览模式时返回 null
    if (isInPreview) {
        return null
    }

    var wallpaper by remember { mutableStateOf<Drawable?>(null) }

    // 在 compose 中加载壁纸
    LaunchedEffect(Unit) {
        Log.d("WallpaperHelper", "Loading wallpaper...")
        wallpaper = WallpaperHelper.getWallpaper(context)
        Log.d("WallpaperHelper", "Wallpaper loaded: $wallpaper")
    }

    return wallpaper
}
