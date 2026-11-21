package com.nahueltaibo.pagecast.data

import org.xmlpull.v1.XmlPullParser

/** Reads the Pagecast episodes out of an RSS feed. Takes the parser so JVM tests can supply one. */
object FeedParser {
    // Must match PAGECAST_NS in the pipeline's feed.py.
    const val PAGECAST_NS = "https://github.com/nahueltaibo/article-to-podcast/ns/1"

    fun parse(xml: String, parser: XmlPullParser): List<RemoteEpisode> {
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(xml.reader())
        val episodes = mutableListOf<RemoteEpisode>()
        var title = ""
        var guid = ""
        var presentation: String? = null
        var source: String? = null
        val categories = mutableListOf<String>()
        var inItem = false
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> if (parser.name == "item") {
                    inItem = true; title = ""; guid = ""; presentation = null; source = null; categories.clear()
                } else if (inItem) {
                    val pagecast = parser.namespace == PAGECAST_NS
                    when {
                        parser.name == "title" && !pagecast -> title = parser.nextText()
                        parser.name == "guid" -> guid = parser.nextText()
                        parser.name == "category" && !pagecast -> categories += parser.nextText().trim()
                        parser.name == "source" && pagecast -> source = parser.nextText().trim()
                        parser.name == "presentation" && pagecast -> presentation = parser.getAttributeValue(null, "url")
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name == "item") {
                    inItem = false
                    // Items without a presentation are plain podcast episodes, which Pagecast can't show yet.
                    presentation?.let { episodes += RemoteEpisode(guid.ifEmpty { it }, title, it, source, categories.toList()) }
                }
            }
        }
        return episodes
    }
}
