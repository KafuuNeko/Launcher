package me.kafuuneko.launcher.libs.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import me.kafuuneko.launcher.ui.theme.LauncherTheme

abstract class CoreActivity : ComponentActivity() {
    protected open fun isEnableEdgeToEdge(): Boolean = true

    @Composable
    protected abstract fun ViewContent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView() {
        if (isEnableEdgeToEdge()) {
            enableEdgeToEdge()
        }
        setContent { LauncherTheme(content = getContent()) }
    }

    private fun getContent(): @Composable () -> Unit = { ViewContent() }
}

@Composable
fun ActivityPreview(darkTheme: Boolean, content: @Composable () -> Unit) {
    LauncherTheme(darkTheme = darkTheme, content = content)
}
