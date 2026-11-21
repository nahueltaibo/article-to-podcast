package com.nahueltaibo.pagecast.ui

import android.app.Activity
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
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
import com.nahueltaibo.pagecast.playback.Speed
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
    var showSpeed by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(controller.playbackParameters.speed) }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackParametersChanged(params: PlaybackParameters) {
                speed = params.speed
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
    val chapterTitle = presentation.chapters.getOrNull(chapterIndex)?.title
    val figureStarts = presentation.figureStartsMs
    val figureNumber = figureStarts.count { it <= shown }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val noRipple = remember { MutableInteractionSource() }
    // Panels sit on the empty bands around the figure. The backing keeps them readable if they overlap it.
    val panel = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
    val fraction = if (duration > 0) (shown.toFloat() / duration).coerceIn(0f, 1f) else 0f

    val playPause: @Composable (Int) -> Unit = { size ->
        FilledIconButton(
            onClick = { if (isPlaying) controller.pause() else controller.play(); touch() },
            modifier = Modifier.size(size.dp),
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size((size / 2).dp),
            )
        }
    }
    val seekBar: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier) {
            FigureMarks(figureStarts, duration)
            Slider(
                value = shown.coerceIn(0, duration).toFloat(),
                onValueChange = { seekingTo = it.toLong(); touch() },
                onValueChangeFinished = {
                    seekingTo?.let(controller::seekTo)
                    seekingTo = null
                },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            )
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = noRipple, indication = null) {
                    controlsVisible = !controlsVisible
                    touch()
                },
        ) {
            AsyncImage(
                model = presentation.imageAt(shown)?.let(episode::file),
                imageLoader = imageLoader,
                contentDescription = chapterTitle,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )

            AnimatedVisibility(!controlsVisible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomStart)) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Box(Modifier.fillMaxWidth(fraction).height(3.dp).background(MaterialTheme.colorScheme.tertiary))
                }
            }

            AnimatedVisibility(controlsVisible, enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    if (landscape) {
                        FilledTonalIconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                        Row(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(panel)
                                .clickable(interactionSource = noRipple, indication = null) { touch() }
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { controller.seekBack(); touch() }) {
                                Icon(Icons.Default.Replay30, "Back 30 seconds")
                            }
                            playPause(48)
                            IconButton(onClick = { controller.seekForward(); touch() }) {
                                Icon(Icons.Default.Forward30, "Forward 30 seconds")
                            }
                            Text(formatTime(shown), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 8.dp))
                            seekBar(Modifier.weight(1f))
                            Text(
                                "-${formatTime(duration - shown)}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                            TextButton(onClick = { showSpeed = true; touch() }) { Text(Speed.label(speed)) }
                            IconButton(onClick = { showChapters = true; touch() }) {
                                Icon(Icons.AutoMirrored.Filled.List, "Chapters")
                            }
                        }
                    } else {
                        Row(
                            Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .background(panel)
                                .clickable(interactionSource = noRipple, indication = null) { touch() }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    presentation.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (chapterTitle != null && chapterTitle != presentation.title) {
                                    Text(
                                        chapterTitle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        Column(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(panel)
                                .clickable(interactionSource = noRipple, indication = null) { touch() }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            if (figureNumber > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                                    Icon(
                                        Icons.Outlined.Image,
                                        null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Figure $figureNumber of ${figureStarts.size}",
                                        color = MaterialTheme.colorScheme.tertiary,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                            seekBar(Modifier.fillMaxWidth())
                            Row {
                                Text(formatTime(shown), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "-${formatTime(duration - shown)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(onClick = { controller.seekBack(); touch() }, Modifier.size(52.dp)) {
                                    Icon(Icons.Default.Replay30, "Back 30 seconds", modifier = Modifier.size(32.dp))
                                }
                                playPause(76)
                                IconButton(onClick = { controller.seekForward(); touch() }, Modifier.size(52.dp)) {
                                    Icon(Icons.Default.Forward30, "Forward 30 seconds", modifier = Modifier.size(32.dp))
                                }
                            }
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                TextButton(onClick = { showSpeed = true; touch() }) { Text(Speed.label(speed)) }
                                TextButton(onClick = { showChapters = true; touch() }) {
                                    Icon(Icons.AutoMirrored.Filled.List, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Chapters")
                                }
                                TextButton(onClick = ::stop) {
                                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Stop")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSpeed) {
        SpeedSheet(
            speed = speed,
            remainingMs = Speed.remainingMs(duration, shown, speed),
            onSpeed = { controller.setPlaybackSpeed(it); touch() },
            onDismiss = { showSpeed = false },
        )
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
                        colors = ListItemDefaults.colors(
                            containerColor = if (index == chapterIndex) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                        ),
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

/** One tick per figure, lined up with the seek bar below it. */
@Composable
private fun FigureMarks(startsMs: List<Long>, durationMs: Long) {
    val color = MaterialTheme.colorScheme.tertiary
    Canvas(Modifier.fillMaxWidth().height(8.dp)) {
        if (durationMs <= 0) return@Canvas
        val width = 3.dp.toPx()
        startsMs.forEach { start ->
            val x = (size.width - width) * (start.toFloat() / durationMs).coerceIn(0f, 1f)
            drawRoundRect(color, Offset(x, 0f), Size(width, size.height), CornerRadius(width / 2))
        }
    }
}

/** Steps of 0.1 for fine control, plus the presets most listeners use. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSheet(speed: Float, remainingMs: Long, onSpeed: (Float) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Playback speed", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                FilledTonalIconButton(onClick = { onSpeed(Speed.step(speed, -1)) }, enabled = speed > Speed.MIN, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.Remove, "Slower")
                }
                Text(
                    Speed.label(speed),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(140.dp),
                )
                FilledTonalIconButton(onClick = { onSpeed(Speed.step(speed, 1)) }, enabled = speed < Speed.MAX, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.Add, "Faster")
                }
            }
            Text(
                "${formatTime(remainingMs)} left at this speed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(16.dp))
            // Equal-width chips, so every preset is visible without scrolling.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Speed.PRESETS.forEach { preset ->
                    FilterChip(
                        selected = Speed.clamp(speed) == preset,
                        onClick = { onSpeed(preset) },
                        label = {
                            Text(Speed.label(preset), maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        },
                        modifier = Modifier.weight(1f),
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
