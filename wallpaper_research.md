# Easy Launcher 壁纸获取与设置机制调研报告

## 概述

本文档分析了 Easy Launcher 应用如何获取手机壁纸并设置给应用当背景的实现机制。

## 核心实现机制

### 1. 显示系统壁纸背景

**关键配置位置：** `app/src/main/res/values/styles.xml`

Easy Launcher 通过 Android 的主题属性 `android:windowShowWallpaper` 来实现显示系统壁纸背景：

```xml
<style name="Theme.EasyLauncher" parent="@style/Base.Theme.EET">
    <item name="android:windowBackground">@android:color/transparent</item>
    <item name="android:windowNoTitle">true</item>
    <item name="android:windowShowWallpaper">true</item>
    <item name="android:colorBackgroundCacheHint">@null</item>
    <item name="android:windowTranslucentStatus">false</item>
    <item name="android:windowTranslucentNavigation">false</item>
    <item name="android:windowDrawsSystemBarBackgrounds">true</item>
    <item name="android:statusBarColor">#00000000</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
</style>
```

**实现原理：**
- `android:windowBackground="@android:color/transparent"` - 设置窗口背景透明
- `android:windowShowWallpaper="true"` - 显示系统壁纸作为窗口背景
- 透明状态栏和导航栏 - 让壁纸延伸到系统栏区域

该配置同样应用在不同版本的样式文件中：
- `values/styles.xml` (通用版本)
- `values-night/styles.xml` (夜间模式)
- `values-v29/styles.xml` (Android 29+)
- `values-night-v29/styles.xml` (Android 29+ 夜间模式)

### 2. 壁纸颜色监听机制

**核心代码位置：** `app/src/main/java/easy/launcher/`

#### 2.1 壁纸颜色监听器 (`hs9.java`)

```java
public final class hs9 implements WallpaperManager.OnColorsChangedListener {
    @Override
    public final void onColorsChanged(WallpaperColors wallpaperColors, int i) {
        Timber.f50208a.mo28520d(ue1.m26656l(i, "onColorsChanged: which = "), new Object[0]);
        if (i82.m21558M(this.f29849a).get("theme_use_wallpaper_colors", false)) {
            this.f29850b.f31299c = true;  // 标记需要重建
        }
    }
}
```

**功能说明：**
- 实现 `WallpaperManager.OnColorsChangedListener` 接口
- 监听系统壁纸颜色变化
- 当用户启用了"使用壁纸颜色"主题功能时，标记需要重建 Activity

#### 2.2 生命周期管理 (`is9.java`)

```java
public final class is9 implements h02 {
    public final SoftReference f31297a;  // Launcher 引用
    public final hs9 f31298b;            // 监听器
    public boolean f31299c;              // 是否需要重建

    @Override
    public final void onCreate(ct4 ct4Var) {
        Launcher launcher = (Launcher) this.f31297a.get();
        if (launcher != null) {
            WallpaperManager wm = gi1.getSystemService(launcher, WallpaperManager.class);
            // 添加壁纸颜色变化监听器
            wm.addOnColorsChangedListener(this.f31298b, new Handler(Looper.getMainLooper()));
        }
    }

    @Override
    public final void onDestroy(ct4 ct4Var) {
        Launcher launcher = (Launcher) this.f31297a.get();
        if (launcher != null) {
            WallpaperManager wm = gi1.getSystemService(launcher, WallpaperManager.class);
            // 移除壁纸颜色变化监听器
            wm.removeOnColorsChangedListener(this.f31298b);
        }
    }

    @Override
    public final void onResume(ct4 ct4Var) {
        if (this.f31299c) {
            Launcher launcher = (Launcher) this.f31297a.get();
            if (launcher != null) {
                launcher.recreate();  // 重建 Activity 以应用新主题
            }
            this.f31299c = false;
        }
    }
}
```

**工作流程：**
1. **onCreate**: 获取 WallpaperManager 服务并注册颜色变化监听器
2. **onDestroy**: 移除监听器，避免内存泄漏
3. **onResume**: 如果壁纸颜色发生变化，重建 Activity 以更新主题颜色

### 3. 设置壁纸功能

**核心代码位置：** `app/src/main/java/com/eet/feature/wallpapers/p030ui/WallpaperPreviewActivity.java`

该 Activity 负责预览和设置壁纸：

**权限声明：** `AndroidManifest.xml`
```xml
<uses-permission android:name="android.permission.SET_WALLPAPER"/>
<uses-permission android:name="android.permission.SET_WALLPAPER_HINTS"/>

<intent-filter>
    <action android:name="android.intent.action.SET_WALLPAPER"/>
    <category android:name="android.intent.category.DEFAULT"/>
</intent-filter>
```

**主要功能：**
1. 加载和预览壁纸图片
2. 裁剪和调整壁纸
3. 设置到主屏幕、锁屏或两者
4. 通过异步任务处理壁纸设置操作

## 技术要点总结

### 显示系统壁纸的方式

Easy Launcher 使用 Android 系统提供的 **Window 属性** 来显示壁纸，而不是手动获取壁纸图片：

| 方式 | 优点 | 缺点 |
|------|------|------|
| `windowShowWallpaper` | • 系统原生支持<br>• 性能高效<br>• 自动跟随壁纸变化 | • 需要透明背景<br>• 无法对壁纸进行自定义处理 |
| 手动获取壁纸 | • 可以自定义处理<br>• 可以添加效果 | • 需要手动管理更新<br>• 性能开销较大 |

### 壁纸颜色主题联动

应用实现了壁纸颜色与主题的联动功能：

1. **监听壁纸颜色变化** - 通过 `WallpaperManager.OnColorsChangedListener`
2. **提取壁纸主色调** - 使用 `WallpaperColors` API
3. **动态更新主题** - 重建 Activity 应用新的 Material You 颜色方案
4. **用户可选启用** - 通过设置项 `theme_use_wallpaper_colors` 控制

### 相关文件清单

| 文件路径 | 功能说明 |
|---------|---------|
| `app/src/main/res/values/styles.xml` | 主题配置，定义 `windowShowWallpaper` 属性 |
| `app/src/main/java/easy/launcher/hs9.java` | 壁纸颜色变化监听器 |
| `app/src/main/java/easy/launcher/is9.java` | 监听器生命周期管理 |
| `app/src/main/java/easy/launcher/Launcher.java` | 主 Activity，初始化壁纸监听 |
| `app/src/main/java/com/eet/feature/wallpapers/p030ui/WallpaperPreviewActivity.java` | 壁纸预览和设置界面 |
| `app/src/main/java/easy/launcher/g1a.java` | 工作区页面布局，处理背景绘制 |
| `app/src/main/AndroidManifest.xml` | 权限声明和 Activity 配置 |

## 实现原理图

```
┌─────────────────────────────────────────────────────────┐
│                   Easy Launcher 启动                     │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│           应用 Theme.EasyLauncher 主题                   │
│  • windowBackground = transparent                       │
│  • windowShowWallpaper = true                          │
│  • 状态栏和导航栏透明                                    │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│              Android 系统显示壁纸层                      │
│        (由 WallpaperManager 管理的系统壁纸)              │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│              WallpaperColor 监听器                       │
│  • 注册 OnColorsChangedListener                         │
│  • 监听壁纸颜色变化                                     │
│  • 触发主题重建                                         │
└─────────────────────────────────────────────────────────┘
```

## 关键 API 说明

### WallpaperManager

系统壁纸管理器，主要方法：

| 方法 | 说明 |
|------|------|
| `getInstance(Context)` | 获取 WallpaperManager 实例 |
| `addOnColorsChangedListener()` | 添加壁纸颜色变化监听器 |
| `removeOnColorsChangedListener()` | 移除监听器 |
| `getWallpaperColors(int)` | 获取指定壁纸的颜色信息 |
| `setBitmap()` | 设置壁纸图片 |

### WallpaperColors

壁纸颜色信息类：

| 方法 | 说明 |
|------|------|
| `getPrimaryColors()` | 获取主要颜色 |
| `getSecondaryColors()` | 获取次要颜色 |
| `getTertiaryColors()` | 获取第三色调 |

## 开发建议

如需实现类似功能，建议：

1. **显示系统壁纸**: 使用 `android:windowShowWallpaper` 主题属性
2. **监听壁纸变化**: 实现 `WallpaperManager.OnColorsChangedListener`
3. **颜色主题联动**: 提取壁纸主色调并应用到 Material You 主题
4. **生命周期管理**: 在 Activity 生命周期中正确注册和移除监听器

## 参考资料

- [Android WallpaperManager 文档](https://developer.android.com/reference/android/app/WallpaperManager)
- [Window 属性配置](https://developer.android.com/guide/topics/ui/look-and-feel/themes)
- Material You 动态颜色系统
