package com.nahueltaibo.pagecast.ui

import com.nahueltaibo.pagecast.data.LocalEpisode
import com.nahueltaibo.pagecast.data.Presentation
import com.nahueltaibo.pagecast.data.RemoteEpisode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.time.LocalDate

class LibraryFilterTest {
    private val today = LocalDate.of(2026, 11, 21)

    private fun row(title: String, published: LocalDate?, downloaded: Boolean = false, category: String = "Science"): EpisodeRow {
        val remote = RemoteEpisode(title, title, "https://h/$title/presentation.json", "Anthropic", listOf(category), published)
        val local = if (downloaded) {
            LocalEpisode(File(title), Presentation(title, title, "", "", 1000, "a.mp3", null, null, null, emptyList(), emptyMap(), emptyList()))
        } else {
            null
        }
        return EpisodeRow(title, title, remote, local)
    }

    private val rows = listOf(
        row("beta", today.minusDays(1), downloaded = true),
        row("Alpha", today.minusDays(5), category = "Economics"),
        row("gamma", today.minusDays(20)),
        row("delta", today.minusDays(90)),
        row("undated", null),
    )

    private fun titles(filter: LibraryFilter) = filter.apply(rows, today).map { it.title }

    @Test
    fun newestFirstPutsUndatedLast() {
        assertEquals(listOf("beta", "Alpha", "gamma", "delta", "undated"), titles(LibraryFilter()))
    }

    @Test
    fun oldestFirstAndTitleOrder() {
        assertEquals(listOf("delta", "gamma", "Alpha", "beta", "undated"), titles(LibraryFilter(sort = Sort.OLDEST)))
        assertEquals(listOf("Alpha", "beta", "delta", "gamma", "undated"), titles(LibraryFilter(sort = Sort.TITLE)))
    }

    @Test
    fun dateWindowsIncludeTheirEdgeAndDropUndated() {
        assertEquals(listOf("beta"), titles(LibraryFilter(window = Window.DAY)))
        assertEquals(listOf("beta", "Alpha"), titles(LibraryFilter(window = Window.WEEK)))
        assertEquals(listOf("beta", "Alpha", "gamma"), titles(LibraryFilter(window = Window.MONTH)))
    }

    @Test
    fun filtersCombine() {
        assertEquals(listOf("beta"), titles(LibraryFilter(downloaded = true)))
        assertEquals(listOf("Alpha"), titles(LibraryFilter(window = Window.WEEK, category = "Economics")))
    }

    @Test
    fun continueListeningKeepsStartedDownloadsByLastPlayed() {
        val all = rows + row("epsilon", today, downloaded = true)
        val positions = mapOf("beta" to 5_000L, "epsilon" to 1_000L, "gamma" to 9_000L)
        val played = mapOf("beta" to 1L, "epsilon" to 2L)
        val result = continueListening(all, { positions[it] ?: 0 }, { played[it] ?: 0 })
        // gamma has a position but isn't downloaded, so it can't play.
        assertEquals(listOf("epsilon", "beta"), result.map { it.title })
    }
}
