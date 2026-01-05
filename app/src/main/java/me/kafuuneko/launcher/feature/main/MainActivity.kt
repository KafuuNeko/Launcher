package me.kafuuneko.launcher.feature.main

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import me.kafuuneko.launcher.feature.main.presentation.MainUiIntent
import me.kafuuneko.launcher.feature.main.presentation.MainUiState
import me.kafuuneko.launcher.feature.main.presentation.MainViewEvent
import me.kafuuneko.launcher.feature.main.presentation.PageType
import me.kafuuneko.launcher.feature.main.ui.MainLayout
import me.kafuuneko.launcher.libs.core.CoreActivityWithEvent
import me.kafuuneko.launcher.libs.core.IViewEvent

class MainActivity : CoreActivityWithEvent() {
    private val mViewModel by viewModels<MainViewModel>()

    override fun getViewEventFlow() = mViewModel.viewEventFlow

    @Composable
    override fun ViewContent() {
        val uiState by mViewModel.uiStateFlow.collectAsState()

        LaunchedEffect(Unit) {
            mViewModel.emit(MainUiIntent.Init)
        }

        BackHandler {
            when (val state = uiState) {
                is MainUiState.Normal -> {
                    if (state.currentPage != PageType.HOME) mViewModel.emit(MainUiIntent.GoBack)
                }

                else -> Unit
            }
        }

        MainLayout(
            uiState = uiState,
            emitIntent = { intent -> mViewModel.emit(intent) }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel.emit(MainUiIntent.RefreshApps)
    }

    override fun onResume() {
        super.onResume()
        mViewModel.emit(MainUiIntent.Resume)
    }

    override suspend fun onReceivedViewEvent(viewEvent: IViewEvent) {
        super.onReceivedViewEvent(viewEvent)
        when (viewEvent) {
            is MainViewEvent.StartApp -> {
                startActivity(viewEvent.intent)
            }

            is MainViewEvent.ShowAppInfo -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${viewEvent.packageName}")
                }
                startActivity(intent)
            }

            MainViewEvent.OpenWallpaperPicker -> {
                val intent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                    putExtra("wallpaper-component", "me.kafuuneko.launcher")
                }
                startActivity(intent)
            }
        }
    }
}
