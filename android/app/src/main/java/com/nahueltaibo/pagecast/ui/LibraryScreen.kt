package com.nahueltaibo.pagecast.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.nahueltaibo.pagecast.data.LocalEpisode
import com.nahueltaibo.pagecast.data.RemoteEpisode

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
    resumeAt: (String) -> Long,
) {
    var adding by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pagecast") },
                actions = {
                    if (state.refreshing) CircularProgressIndicator(Modifier.size(24.dp))
                    else IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh") }
                    IconButton(onClick = { adding = true }) { Icon(Icons.Default.Add, "Add podcast") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.feeds.isEmpty()) {
                Text(
                    "Add a Pagecast feed with + to get started.",
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            LazyColumn {
                items(state.rows, key = { it.id }) { row ->
                    EpisodeItem(row, state.downloads[row.id], resumeAt(row.id), onDownload, onDelete, onOpen)
                    HorizontalDivider()
                }
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
private fun EpisodeItem(
    row: EpisodeRow,
    progress: Float?,
    resumeMs: Long,
    onDownload: (RemoteEpisode) -> Unit,
    onDelete: (LocalEpisode) -> Unit,
    onOpen: (LocalEpisode) -> Unit,
) {
    val local = row.local
    val subtitle = when {
        progress != null -> "Downloading ${(progress * 100).toInt()}%"
        local == null -> "Not downloaded"
        resumeMs > 0 -> "Resume at ${formatTime(resumeMs)} of ${formatTime(local.presentation.durationMs)}"
        else -> formatTime(local.presentation.durationMs)
    }
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(enabled = local != null) { local?.let(onOpen) },
        headlineContent = { Text(row.title) },
        supportingContent = { Text(subtitle) },
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
