package com.nahueltaibo.pagecast.data

import org.xmlpull.v1.XmlPullParser
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Reads the Pagecast episodes out of an RSS feed. Takes the parser so JVM tests can supply one. */
object FeedParser {
    // Must match PAGECAST_NS in the pipeline's feed.py.
    const val PAGECAST_NS = "https://github.com/nahueltaibo/article-to-podcast/ns/1"
    private const val ITUNES_NS = "http://www.itunes.com/dtds/podcast-1.0.dtd"

    fun parse(xml: String, parser: XmlPullParser): List<RemoteEpisode> {
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(xml.reader())
        val episodes = mutableListOf<RemoteEpisode>()
        var title = ""
        var guid = ""
        var presentation: String? = null
        var source: String? = null
        var published: LocalDate? = null
        var duration: Long? = null
        val categories = mutableListOf<String>()
        var inItem = false
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> if (parser.name == "item") {
                    inItem = true; title = ""; guid = ""; presentation = null; source = null; published = null; duration = null; categories.clear()
                } else if (inItem) {
                    val pagecast = parser.namespace == PAGECAST_NS
                    when {
                        parser.name == "title" && !pagecast -> title = parser.nextText()
                        parser.name == "guid" -> guid = parser.nextText()
                        parser.name == "pubDate" -> published = parseDate(parser.nextText())
                        parser.name == "duration" && parser.namespace == ITUNES_NS -> duration = parseDuration(parser.nextText())
                        parser.name == "category" && !pagecast -> categories += parser.nextText().trim()
                        parser.name == "source" && pagecast -> source = parser.nextText().trim()
                        parser.name == "presentation" && pagecast -> presentation = parser.getAttributeValue(null, "url")
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name == "item") {
                    inItem = false
                    // Items without a presentation are plain podcast episodes, which Pagecast can't show yet.
                    presentation?.let { episodes += RemoteEpisode(guid.ifEmpty { it }, title, it, source, categories.toList(), published, duration) }
                }
            }
        }
        return episodes
    }

    /** itunes:duration is "HH:MM:SS", "MM:SS", or plain seconds. */
    private fun parseDuration(text: String): Long? {
        val parts = text.trim().split(":").map { it.toLongOrNull() ?: return null }
        if (parts.isEmpty() || parts.size > 3) return null
        return parts.fold(0L) { total, part -> total * 60 + part } * 1000
    }

    /** RSS dates look like "Tue, 17 Nov 2026 00:00:00 +0000". A bad date shouldn't drop the episode. */
    private fun parseDate(text: String): LocalDate? = runCatching {
        ZonedDateTime.parse(text.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).withZoneSameInstant(ZoneOffset.UTC).toLocalDate()
    }.getOrNull()
}
