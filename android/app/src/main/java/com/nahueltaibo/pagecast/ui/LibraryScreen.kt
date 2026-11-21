package com.nahueltaibo.pagecast.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nahueltaibo.pagecast.data.LocalEpisode
import com.nahueltaibo.pagecast.data.RemoteEpisode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryState,
    onAddFeed: (String) -> Unit,
    onUseRecommended: () -> Unit,
    onRefresh: () -> Unit,
    onDownload: (RemoteEpisode) -> Unit,
    onDelete: (LocalEpisode) -> Unit,
    onOpen: (LocalEpisode) -> Unit,
    onDismissError: () -> Unit,
    onSettings: () -> Unit,
    onFilter: (LibraryFilter) -> Unit,
    resumeAt: (String) -> Long,
    lastPlayed: (String) -> Long,
) {
    var adding by remember { mutableStateOf(false) }
    val filter = state.filter
    val categories = state.rows.flatMap { it.categories }.distinct().sorted()
    // Recomputed on every return from the player, which is when positions change.
    val resuming = continueListening(state.rows, resumeAt, lastPlayed).firstOrNull()
    val shown = filter.apply(state.rows, LocalDate.now())
    val noSources = state.feeds.isEmpty() && state.rows.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pagecast") },
                actions = {
                    if (state.refreshing) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    else IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh") }
                    IconButton(onClick = { adding = true }) { Icon(Icons.Default.Add, "Add a source") }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
                },
            )
        },
    ) { padding ->
        if (noSources) {
            EmptyState(
                icon = Icons.Default.Add,
                title = "Add your first source",
                text = "Paste the address of any podcast or audiobook feed. Feeds made for Pagecast also show their figures while you listen.",
                modifier = Modifier.padding(padding),
            ) {
                Button(onClick = { adding = true }, Modifier.fillMaxWidth().height(52.dp)) { Text("Add a source") }
                OutlinedButton(onClick = onUseRecommended, Modifier.fillMaxWidth().height(52.dp)) { Text("Use the recommended feed") }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                if (resuming != null) {
                    item(key = "continue") {
                        SectionHeader("Continue")
                        ContinueCard(resuming, resumeAt(resuming.id), onOpen)
                    }
                }
                item {
                    SectionHeader("Episodes")
                    FilterChips(filter, categories, onFilter)
                    Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (shown.size == 1) "1 episode" else "${shown.size} episodes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        SortMenu(filter.sort) { onFilter(filter.copy(sort = it)) }
                    }
                }
                if (shown.isEmpty() && state.rows.isNotEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.FilterList,
                            title = "Nothing matches these filters",
                            text = "Remove a filter to see more episodes.",
                            modifier = Modifier.padding(top = 32.dp),
                        ) {
                            Button(onClick = { onFilter(LibraryFilter(sort = filter.sort)) }) { Text("Clear filters") }
                        }
                    }
                }
                items(shown, key = { it.id }) { row ->
                    EpisodeItem(row, state.downloads[row.id], resumeAt(row.id), onDownload, onDelete, onOpen)
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    if (adding) AddSourceSheet(onAdd = { onAddFeed(it); adding = false }, onDismiss = { adding = false })
    state.error?.let { message ->
        ErrorDialog(message, onRetry = { onDismissError(); onRefresh() }, onDismiss = onDismissError)
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit,
) {
    Column(
        modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
            Icon(icon, null, Modifier.padding(18.dp).size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        actions()
    }
}

/** The episode played most recently, one tap from resuming. */
@Composable
private fun ContinueCard(row: EpisodeRow, resumeMs: Long, onOpen: (LocalEpisode) -> Unit) {
    val local = row.local ?: return
    val duration = local.presentation.durationMs
    Surface(
        onClick = { onOpen(local) },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(row.source, "${durationLabel(duration - resumeMs)} left").joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                LinearProgressIndicator(
                    progress = { resumeMs.toFloat() / duration.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    drawStopIndicator = {},
                )
            }
            Spacer(Modifier.width(12.dp))
            FilledIconButton(onClick = { onOpen(local) }, Modifier.size(48.dp)) { Icon(Icons.Default.PlayArrow, "Resume") }
        }
    }
}

@Composable
private fun SortMenu(sort: Sort, onSort: (Sort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, null, Modifier.size(18.dp))
            Text(sort.label, Modifier.padding(start = 6.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Sort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    trailingIcon = { if (option == sort) Icon(Icons.Default.Check, null) },
                    onClick = { onSort(option); open = false },
                )
            }
        }
    }
}

/** Downloaded, one date window, and one topic. The two menus keep every filter on screen at any width. */
@Composable
private fun FilterChips(filter: LibraryFilter, categories: List<String>, onFilter: (LibraryFilter) -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = filter.downloaded,
            onClick = { onFilter(filter.copy(downloaded = !filter.downloaded)) },
            label = { Text("Downloaded") },
            leadingIcon = if (filter.downloaded) { { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) } } else null,
        )
        MenuChip(
            label = "Date",
            options = Window.entries.map { it.label },
            selected = filter.window?.label,
            onSelect = { label -> onFilter(filter.copy(window = Window.entries.firstOrNull { it.label == label })) },
        )
        if (categories.size > 1) {
            MenuChip("Topic", categories, filter.category) { onFilter(filter.copy(category = it)) }
        }
    }
}

/** A chip that opens a single-choice menu. It shows the choice, and "Any" clears it. */
@Composable
private fun MenuChip(label: String, options: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { open = true },
            label = { Text(selected ?: label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(18.dp)) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            (listOf<String?>(null) + options).forEach { option ->
                DropdownMenuItem(
                    text = { Text(option ?: "Any ${label.lowercase()}") },
                    trailingIcon = { if (option == selected) Icon(Icons.Default.Check, null) },
                    onClick = { onSelect(option); open = false },
                )
            }
        }
    }
}

@Composable
private fun EpisodeItem(
    row: EpisodeRow,
    progress: Float?,
    resumeMs: Long,
    onDownload: (RemoteEpisode) -> Unit,
    onDelete: (LocalEpisode) -> Unit,
    onOpen: (LocalEpisode) -> Unit,
) {
    val local = row.local
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(enabled = local != null) { local?.let(onOpen) },
        overlineContent = tagLine(row)?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        headlineContent = { Text(row.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        // Not downloaded needs no text: the download button already says so.
        supportingContent = when {
            progress != null -> { { Text("Downloading ${(progress * 100).toInt()}%") } }
            local == null -> null
            resumeMs > 0 -> {
                {
                    Column {
                        Text("${durationLabel(local.presentation.durationMs - resumeMs)} left")
                        LinearProgressIndicator(
                            progress = { resumeMs.toFloat() / local.presentation.durationMs.coerceAtLeast(1) },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            drawStopIndicator = {},
                        )
                    }
                }
            }
            else -> null
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    progress != null -> CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.padding(12.dp).size(24.dp),
                        strokeWidth = 3.dp,
                    )
                    local != null -> {
                        EpisodeMenu(onDelete = { onDelete(local) })
                        IconButton(onClick = { onOpen(local) }) { Icon(Icons.Default.PlayArrow, "Play") }
                    }
                    row.remote != null -> IconButton(onClick = { onDownload(row.remote) }) {
                        Icon(Icons.Default.Download, "Download", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
    )
}

/** Removing a download sits behind a menu, so it can't be hit by accident next to play. */
@Composable
private fun EpisodeMenu(onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, "More options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Remove download") }, onClick = { open = false; onDelete() })
        }
    }
}

/** "Anthropic · Science · Nov 17, 2026 · 31 min", or null for an episode with nothing to show. */
private fun tagLine(row: EpisodeRow): String? =
    (listOfNotNull(row.source) + row.categories + listOfNotNull(row.publishedAt?.format(dateFormat), row.durationMs?.let(::durationLabel)))
        .joinToString(" · ").ifEmpty { null }

/** Rounded the way podcast apps list episodes: "45 min", "1 h 5 min". */
fun durationLabel(ms: Long): String {
    val minutes = ((ms + 30_000) / 60_000).coerceAtLeast(1)
    return if (minutes < 60) "$minutes min" else if (minutes % 60 == 0L) "${minutes / 60} h" else "${minutes / 60} h ${minutes % 60} min"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSourceSheet(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add a source", style = MaterialTheme.typography.titleLarge)
            Text(
                "Any podcast or audiobook feed works. Feeds made for Pagecast also show their figures.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Feed address") },
                    placeholder = { Text("https://") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { clipboard.getText()?.text?.let { url = it.trim() } }) { Text("Paste") }
            }
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, Modifier.weight(1f).height(52.dp)) { Text("Cancel") }
                Button(onClick = { onAdd(url) }, Modifier.weight(1f).height(52.dp), enabled = url.isNotBlank()) { Text("Add") }
            }
        }
    }
}

/** Says what happened in plain words. The raw message stays one tap away for bug reports. */
@Composable
private fun ErrorDialog(message: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    var details by remember { mutableStateOf(false) }
    val download = message.startsWith("Download failed")
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(if (download) "Download failed" else "Couldn't reach a source") },
        text = {
            Column {
                Text(
                    if (download) "The episode was not saved. Check your connection and try again."
                    else "Check the address and your connection, then try again.",
                )
                TextButton(onClick = { details = !details }, Modifier.padding(top = 4.dp)) {
                    Text(if (details) "Hide technical details" else "Technical details")
                }
                AnimatedVisibility(details) {
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                        Text(message, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            if (download) TextButton(onClick = onDismiss) { Text("OK") }
            else TextButton(onClick = onRetry) { Text("Try again") }
        },
        dismissButton = if (download) null else { { TextButton(onClick = onDismiss) { Text("Close") } } },
    )
}

fun formatTime(ms: Long): String {
    val total = ms.coerceAtLeast(0) / 1000
    val h = total / 3600
    val m = total % 3600 / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
