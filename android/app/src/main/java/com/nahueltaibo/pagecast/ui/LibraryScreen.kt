package com.nahueltaibo.pagecast.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val resuming = continueListening(state.rows, resumeAt, lastPlayed)
    val shown = filter.apply(state.rows, LocalDate.now())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pagecast") },
                actions = {
                    if (state.refreshing) CircularProgressIndicator(Modifier.size(24.dp))
                    else IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh") }
                    IconButton(onClick = { adding = true }) { Icon(Icons.Default.Add, "Add podcast") }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            if (state.feeds.isEmpty()) {
                item {
                    Text(
                        "Add a Pagecast feed with +, or turn on the recommended feed in settings.",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (resuming.isNotEmpty()) {
                item { SectionHeader("Continue listening") }
                items(resuming, key = { "continue:${it.id}" }) { row ->
                    EpisodeItem(row, state.downloads[row.id], resumeAt(row.id), onDownload, onDelete, onOpen)
                    HorizontalDivider()
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader("Episodes")
                    Spacer(Modifier.weight(1f))
                    SortMenu(filter.sort) { onFilter(filter.copy(sort = it)) }
                }
            }
            item { FilterChips(filter, categories, onFilter) }
            if (shown.isEmpty() && state.rows.isNotEmpty()) {
                item {
                    Text("No episodes match these filters.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
            items(shown, key = { it.id }) { row ->
                EpisodeItem(row, state.downloads[row.id], resumeAt(row.id), onDownload, onDelete, onOpen)
                HorizontalDivider()
            }
        }
    }

    if (adding) AddFeedDialog(onAdd = { onAddFeed(it); adding = false }, onDismiss = { adding = false })
    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            confirmButton = { TextButton(onClick = onDismissError) { Text("OK") } },
            text = { Text(message) },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp), style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun SortMenu(sort: Sort, onSort: (Sort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.padding(top = 8.dp, end = 8.dp)) {
        TextButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, null)
            Text(sort.label, Modifier.padding(start = 8.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Sort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = { if (option == sort) Icon(Icons.Default.Check, null) },
                    onClick = { onSort(option); open = false },
                )
            }
        }
    }
}

/** Downloaded and one date window, then one area. Tapping a selected chip clears it. */
@Composable
private fun FilterChips(filter: LibraryFilter, categories: List<String>, onFilter: (LibraryFilter) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = filter.downloaded,
            onClick = { onFilter(filter.copy(downloaded = !filter.downloaded)) },
            label = { Text("Downloaded") },
        )
        Window.entries.forEach { w ->
            FilterChip(
                selected = filter.window == w,
                onClick = { onFilter(filter.copy(window = if (filter.window == w) null else w)) },
                label = { Text(w.label) },
            )
        }
        if (categories.size > 1) {
            VerticalDivider(Modifier.size(width = 1.dp, height = 24.dp))
            categories.forEach { c ->
                FilterChip(
                    selected = filter.category == c,
                    onClick = { onFilter(filter.copy(category = if (filter.category == c) null else c)) },
                    label = { Text(c) },
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
        overlineContent = tagLine(row)?.let { { Text(it) } },
        headlineContent = { Text(row.title) },
        // Not downloaded needs no text: the download button already says so.
        supportingContent = when {
            progress != null -> { { Text("Downloading ${(progress * 100).toInt()}%") } }
            local == null -> null
            resumeMs > 0 -> {
                {
                    Column {
                        Text("${formatTime(local.presentation.durationMs - resumeMs)} left")
                        LinearProgressIndicator(
                            progress = { resumeMs.toFloat() / local.presentation.durationMs.coerceAtLeast(1) },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                    }
                }
            }
            else -> { { Text(formatTime(local.presentation.durationMs)) } }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    progress != null -> CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(24.dp))
                    local != null -> {
                        IconButton(onClick = { onDelete(local) }) { Icon(Icons.Default.Delete, "Delete download") }
                        IconButton(onClick = { onOpen(local) }) { Icon(Icons.Default.PlayArrow, "Play") }
                    }
                    row.remote != null -> IconButton(onClick = { onDownload(row.remote) }) {
                        Icon(Icons.Default.Download, "Download")
                    }
                }
            }
        },
    )
}

/** "Anthropic · Science · Nov 17, 2026", or null for an episode with no tags or date. */
private fun tagLine(row: EpisodeRow): String? =
    (listOfNotNull(row.source) + row.categories + listOfNotNull(row.publishedAt?.format(dateFormat)))
        .joinToString(" · ").ifEmpty { null }

@Composable
private fun AddFeedDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add podcast") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Feed URL") },
                placeholder = { Text("http://192.168.1.10:8000/podcast.xml") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onAdd(url) }, enabled = url.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

fun formatTime(ms: Long): String {
    val total = ms.coerceAtLeast(0) / 1000
    val h = total / 3600
    val m = total % 3600 / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
