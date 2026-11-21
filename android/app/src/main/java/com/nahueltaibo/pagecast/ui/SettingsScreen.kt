package com.nahueltaibo.pagecast.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nahueltaibo.pagecast.data.FeedSources
import com.nahueltaibo.pagecast.data.Settings
import com.nahueltaibo.pagecast.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    theme: ThemeMode,
    onTheme: (ThemeMode) -> Unit,
    feeds: List<String>,
    onRemoveFeed: (String) -> Unit,
    onClose: () -> Unit,
) {
    var chime by remember { mutableStateOf(settings.chime) }
    var sources by remember { mutableStateOf(feeds) }
    fun toggle(value: Boolean) {
        settings.chime = value
        chime = value
    }
    BackHandler(onBack = onClose)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ListItem(
                headlineContent = { Text("Appearance") },
                supportingContent = {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = theme == mode,
                                onClick = { onTheme(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                            ) { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        }
                    }
                },
            )
            ListItem(
                modifier = Modifier.clickable { toggle(!chime) },
                headlineContent = { Text("Chime on new media") },
                supportingContent = { Text("Play a short ding when a new image or animation appears.") },
                trailingContent = { Switch(checked = chime, onCheckedChange = ::toggle) },
            )
            ListItem(
                headlineContent = { Text("Sources") },
                supportingContent = {
                    Text(if (sources.isEmpty()) "No sources. Add one from the library." else "Downloaded episodes stay when you remove a source.")
                },
            )
            sources.forEach { url ->
                ListItem(
                    headlineContent = {
                        Text(if (url == FeedSources.DEFAULT) "Pagecast: Anthropic" else url, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    supportingContent = if (url == FeedSources.DEFAULT) ({ Text(url) }) else null,
                    trailingContent = {
                        IconButton(onClick = {
                            onRemoveFeed(url)
                            sources = sources - url
                        }) { Icon(Icons.Default.Delete, "Remove $url") }
                    },
                )
            }
        }
    }
}
