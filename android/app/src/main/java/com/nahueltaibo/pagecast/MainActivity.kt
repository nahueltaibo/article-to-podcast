package com.nahueltaibo.pagecast

import android.content.ComponentName
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.nahueltaibo.pagecast.playback.PlaybackService
import com.nahueltaibo.pagecast.ui.LibraryScreen
import com.nahueltaibo.pagecast.ui.LibraryViewModel
import com.nahueltaibo.pagecast.ui.PagecastTheme
import com.nahueltaibo.pagecast.ui.isDark
import com.nahueltaibo.pagecast.ui.PlayerScreen
import com.nahueltaibo.pagecast.ui.SettingsScreen
import com.nahueltaibo.pagecast.data.Settings

class MainActivity : ComponentActivity() {
    private val viewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = Settings(applicationContext)
        setContent {
            var theme by remember { mutableStateOf(settings.theme) }
            val dark = isDark(theme)
            // System bar icons must contrast with the app's theme, which can differ from the phone's.
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            PagecastTheme(dark) {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val controller = rememberMediaController()
                var openId by rememberSaveable { mutableStateOf<String?>(null) }
                var settingsOpen by rememberSaveable { mutableStateOf(false) }
                val open = openId?.let { id -> state.rows.firstOrNull { it.id == id }?.local }

                if (settingsOpen) {
                    SettingsScreen(
                        settings = settings,
                        theme = theme,
                        onTheme = {
                            settings.theme = it
                            theme = it
                        },
                        onClose = {
                            settingsOpen = false
                            viewModel.refresh()
                        },
                    )
                } else if (open != null && controller != null) {
                    PlayerScreen(
                        episode = open,
                        controller = controller,
                        library = viewModel.library,
                        onClose = { openId = null },
                    )
                } else {
                    LibraryScreen(
                        state = state,
                        onAddFeed = viewModel::addFeed,
                        onRefresh = viewModel::refresh,
                        onDownload = viewModel::download,
                        onDelete = viewModel::delete,
                        onOpen = { openId = it.id },
                        onDismissError = viewModel::clearError,
                        onSettings = { settingsOpen = true },
                        onFilter = viewModel::setFilter,
                        resumeAt = viewModel.library::position,
                        lastPlayed = viewModel.library::lastPlayed,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberMediaController(): MediaController? {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    DisposableEffect(Unit) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({ controller = future.get() }, ContextCompat.getMainExecutor(context))
        onDispose { MediaController.releaseFuture(future) }
    }
    return controller
}
