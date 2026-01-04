package me.kafuuneko.launcher

import android.app.Application
import com.chibatching.kotpref.Kotpref
import me.kafuuneko.launcher.libs.AppLibs
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

class LauncherApp : Application() {
    companion object {
        private const val TAG = "LauncherApp"
    }

    override fun onCreate() {
        super.onCreate()
        Kotpref.init(this)
        startKoin {
            androidContext(this@LauncherApp)
            modules(appModules)
        }
    }
}

private val appModules = module {
    singleOf(::AppLibs)
}
