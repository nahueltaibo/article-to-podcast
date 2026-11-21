package com.nahueltaibo.pagecast.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import com.nahueltaibo.pagecast.data.Settings
import com.nahueltaibo.pagecast.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: Settings, theme: ThemeMode, onTheme: (ThemeMode) -> Unit, onClose: () -> Unit) {
    var chime by remember { mutableStateOf(settings.chime) }
    var recommended by remember { mutableStateOf(settings.recommendedFeed) }
    fun toggleRecommended(value: Boolean) {
        settings.recommendedFeed = value
        recommended = value
    }
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
                modifier = Modifier.clickable { toggleRecommended(!recommended) },
                headlineContent = { Text("Recommended feed") },
                supportingContent = {
                    Text("Narrated articles from Anthropic: Engineering, Economics, Societal Impacts, and Science. Downloaded episodes stay when you turn it off.")
                },
                trailingContent = { Switch(checked = recommended, onCheckedChange = ::toggleRecommended) },
            )
        }
    }
}
