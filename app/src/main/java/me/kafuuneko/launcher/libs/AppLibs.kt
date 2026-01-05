package me.kafuuneko.launcher.libs

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.StringRes
import me.kafuuneko.launcher.R
import org.koin.core.component.KoinComponent
import androidx.core.net.toUri

class AppLibs(
    private val mContext: Context
) : KoinComponent {
    fun getString(@StringRes id: Int, vararg args: Any): String {
        return mContext.resources?.getString(id, *args).toString()
    }

    fun getVersionName(): String {
        return mContext.packageManager
            .getPackageInfo(mContext.packageName, 0)
            .versionName ?: getString(R.string.unknown_version)
    }

    fun jumpToUrl(url: String) {
        Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also {
            mContext.startActivity(it)
        }
    }

    fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val pm = mContext.packageManager
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == mContext.packageName
    }
}
