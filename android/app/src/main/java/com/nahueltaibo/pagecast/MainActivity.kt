package com.nahueltaibo.pagecast

import android.content.ComponentName
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.nahueltaibo.pagecast.ui.PlayerScreen

class MainActivity : ComponentActivity() {
    private val viewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark, so system bar icons stay light even when the phone uses a light theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val controller = rememberMediaController()
                var openId by rememberSaveable { mutableStateOf<String?>(null) }
                val open = openId?.let { id -> state.rows.firstOrNull { it.id == id }?.local }

                if (open != null && controller != null) {
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
                        resumeAt = viewModel.library::position,
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
