package com.nahueltaibo.pagecast.ui

import java.time.LocalDate

/** How far back the date filter reaches. Dates have no time of day, so the last day also keeps yesterday's episodes. */
enum class Window(val label: String, val days: Long) { DAY("Last day", 1), WEEK("Last week", 7), MONTH("Last month", 30) }

enum class Sort(val label: String) { NEWEST("Newest"), OLDEST("Oldest"), TITLE("Title") }

data class LibraryFilter(
    val downloaded: Boolean = false,
    val window: Window? = null,
    val category: String? = null,
    val sort: Sort = Sort.NEWEST,
) {
    fun apply(rows: List<EpisodeRow>, today: LocalDate): List<EpisodeRow> {
        val since = window?.let { today.minusDays(it.days) }
        val kept = rows.filter { row ->
            (!downloaded || row.local != null) &&
                (since == null || row.publishedAt?.let { !it.isBefore(since) } == true) &&
                (category == null || category in row.categories)
        }
        val byTitle = compareBy<EpisodeRow, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
        // Undated episodes go last in both date orders.
        return when (sort) {
            Sort.NEWEST -> kept.sortedWith(compareBy<EpisodeRow> { it.publishedAt == null }
                .thenByDescending { it.publishedAt }.then(byTitle))
            Sort.OLDEST -> kept.sortedWith(compareBy<EpisodeRow> { it.publishedAt == null }
                .thenBy { it.publishedAt }.then(byTitle))
            Sort.TITLE -> kept.sortedWith(byTitle)
        }
    }
}

/** Downloaded episodes started but not finished, most recently played first. Finishing resets the position to 0. */
fun continueListening(rows: List<EpisodeRow>, position: (String) -> Long, lastPlayed: (String) -> Long): List<EpisodeRow> =
    rows.filter { it.local != null && position(it.id) > 0 }.sortedByDescending { lastPlayed(it.id) }
