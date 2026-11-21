package com.nahueltaibo.pagecast.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.kxml2.io.KXmlParser

class FeedParserTest {
    private val feed = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xmlns:pagecast="${FeedParser.PAGECAST_NS}">
         <channel>
          <title>Pagecast</title>
          <item>
           <title>Tagged</title>
           <guid isPermaLink="false">ep1</guid>
           <pubDate>Tue, 17 Nov 2026 00:00:00 +0000</pubDate>
           <itunes:duration>00:31:24</itunes:duration>
           <category>Science</category>
           <category>Economics</category>
           <pagecast:source>Anthropic</pagecast:source>
           <pagecast:presentation url="https://h/episodes/ep1/presentation.json"/>
          </item>
          <item>
           <title>Plain podcast</title>
           <guid>ep2</guid>
          </item>
          <item>
           <title>Untagged</title>
           <pagecast:presentation url="https://h/episodes/ep3/presentation.json"/>
          </item>
         </channel>
        </rss>
    """.trimIndent()

    private val episodes = FeedParser.parse(feed, KXmlParser())

    @Test
    fun keepsOnlyPagecastItems() {
        assertEquals(listOf("Tagged", "Untagged"), episodes.map { it.title })
    }

    @Test
    fun readsSourceAndCategories() {
        assertEquals("Anthropic", episodes[0].source)
        assertEquals(listOf("Science", "Economics"), episodes[0].categories)
    }

    @Test
    fun readsPublishDateAndToleratesItsAbsence() {
        assertEquals(java.time.LocalDate.of(2026, 11, 17), episodes[0].publishedAt)
        assertEquals(null, episodes[1].publishedAt)
    }

    @Test
    fun readsItunesDuration() {
        assertEquals(1_884_000L, episodes[0].durationMs)
        assertEquals(null, episodes[1].durationMs)
    }

    @Test
    fun untaggedItemsFallBackToPresentationUrlAsId() {
        assertEquals("https://h/episodes/ep3/presentation.json", episodes[1].id)
        assertEquals(emptyList<String>(), episodes[1].categories)
    }
}
