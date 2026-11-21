package com.nahueltaibo.pagecast.ui

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.nahueltaibo.pagecast.data.Library
import com.nahueltaibo.pagecast.data.LocalEpisode
import com.nahueltaibo.pagecast.playback.CHIME_AT_MS
import com.nahueltaibo.pagecast.playback.CHIME_FILE
import kotlinx.coroutines.delay

private const val CONTROLS_TIMEOUT_MS = 4_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(episode: LocalEpisode, controller: MediaController, library: Library, onClose: () -> Unit) {
    val context = LocalContext.current
    val presentation = episode.presentation
    val duration = presentation.durationMs

    LaunchedEffect(episode.id) {
        if (controller.currentMediaItem?.mediaId != episode.id) {
            val metadata = MediaMetadata.Builder()
                .setTitle(presentation.title)
                .setArtist(presentation.site)
                .apply { presentation.cover?.let { setArtworkUri(Uri.fromFile(episode.file(it))) } }
                .setExtras(
                    bundleOf(
                        CHIME_AT_MS to presentation.chimeAtMs.toLongArray(),
                        CHIME_FILE to presentation.chimeFile?.let { episode.file(it).path },
                    ),
                )
                .build()
            val item = MediaItem.Builder()
                .setMediaId(episode.id)
                .setUri(Uri.fromFile(episode.file(presentation.playbackFile)))
                .setMediaMetadata(metadata)
                .build()
            controller.setMediaItem(item, library.position(episode.id))
            controller.prepare()
        }
        controller.play()
    }

    var position by remember { mutableLongStateOf(controller.currentPosition) }
    var isPlaying by remember { mutableStateOf(controller.isPlaying) }
    var seekingTo by remember { mutableStateOf<Long?>(null) }
    var controlsVisible by remember { mutableStateOf(false) }
    var lastTouch by remember { mutableLongStateOf(0L) }
    var showChapters by remember { mutableStateOf(false) }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        controller.addListener(listener)
        onDispose {
            controller.removeListener(listener)
            if (controller.currentMediaItem?.mediaId == episode.id) {
                library.savePosition(episode.id, controller.currentPosition)
            }
        }
    }
    LaunchedEffect(controller) {
        while (true) {
            position = controller.currentPosition
            delay(200)
        }
    }
    // Controls stay up while paused, and fade after a few seconds of playback without touches.
    LaunchedEffect(controlsVisible, lastTouch, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(CONTROLS_TIMEOUT_MS)
            controlsVisible = false
        }
    }
    FullScreen()
    BackHandler(onBack = onClose)

    fun touch() {
        lastTouch = System.nanoTime()
    }

    fun stop() {
        library.savePosition(episode.id, controller.currentPosition)
        controller.pause()
        controller.stop()
        controller.clearMediaItems()
        onClose()
    }

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(if (Build.VERSION.SDK_INT >= 28) ImageDecoderDecoder.Factory() else GifDecoder.Factory()) }
            .crossfade(true)
            .build()
    }
    val shown = seekingTo ?: position
    val chapterIndex = presentation.chapterIndexAt(shown)
    val noRipple = remember { MutableInteractionSource() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = noRipple, indication = null) {
                controlsVisible = true
                touch()
            },
    ) {
        AsyncImage(
            model = presentation.imageAt(shown)?.let(episode::file),
            imageLoader = imageLoader,
            contentDescription = presentation.chapters.getOrNull(chapterIndex)?.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(controlsVisible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(interactionSource = noRipple, indication = null) { controlsVisible = false }
                    .safeDrawingPadding(),
            ) {
                Row(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                    Column(Modifier.weight(1f)) {
                        Text(presentation.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        presentation.chapters.getOrNull(chapterIndex)?.let {
                            Text(
                                it.title,
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    IconButton(onClick = { showChapters = true; touch() }) {
                        Icon(Icons.AutoMirrored.Filled.List, "Chapters", tint = Color.White)
                    }
                }

                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { controller.seekBack(); touch() }, Modifier.size(56.dp)) {
                        Icon(Icons.Default.Replay30, "Back 30 seconds", tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                    IconButton(
                        onClick = { if (isPlaying) controller.pause() else controller.play(); touch() },
                        Modifier.size(80.dp),
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp),
                        )
                    }
                    IconButton(onClick = { controller.seekForward(); touch() }, Modifier.size(56.dp)) {
                        Icon(Icons.Default.Forward30, "Forward 30 seconds", tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                }

                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
                    Slider(
                        value = shown.coerceIn(0, duration).toFloat(),
                        onValueChange = { seekingTo = it.toLong(); touch() },
                        onValueChangeFinished = {
                            seekingTo?.let(controller::seekTo)
                            seekingTo = null
                        },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTime(shown), color = Color.White)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = ::stop) {
                            Icon(Icons.Default.Stop, null, tint = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("Stop", color = Color.White)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(formatTime(duration), color = Color.White)
                    }
                }
            }
        }
    }

    if (showChapters) {
        ModalBottomSheet(onDismissRequest = { showChapters = false }) {
            LazyColumn {
                itemsIndexed(presentation.chapters) { index, chapter ->
                    ListItem(
                        modifier = Modifier.clickable {
                            controller.seekTo(chapter.startMs)
                            showChapters = false
                            touch()
                        },
                        colors = if (index == chapterIndex) {
                            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        } else {
                            ListItemDefaults.colors()
                        },
                        leadingContent = {
                            AsyncImage(
                                model = chapter.image?.let(episode::file),
                                imageLoader = imageLoader,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(width = 72.dp, height = 48.dp),
                            )
                        },
                        headlineContent = { Text(chapter.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(formatTime(chapter.startMs)) },
                    )
                }
            }
        }
    }
}

/** Hides the status and navigation bars while the player is open. A swipe from the edge brings them back briefly. */
@Composable
private fun FullScreen() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as Activity).window
        val insets = WindowCompat.getInsetsController(window, view)
        insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insets.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { insets.show(WindowInsetsCompat.Type.systemBars()) }
    }
}
