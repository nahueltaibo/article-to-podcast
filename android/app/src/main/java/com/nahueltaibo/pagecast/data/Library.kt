package com.nahueltaibo.pagecast.data

import android.content.Context
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** An episode listed in a feed. */
data class RemoteEpisode(val id: String, val title: String, val presentationUrl: String)

/** An episode fully downloaded to [dir]. */
data class LocalEpisode(val dir: File, val presentation: Presentation) {
    val id get() = presentation.id
    fun file(relative: String) = File(dir, relative)
}

/** Feeds, downloads, and playback positions, all in app storage. */
class Library(context: Context) {
    private val prefs = context.getSharedPreferences("library", Context.MODE_PRIVATE)
    private val root = File(context.filesDir, "episodes")
    private val http = OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build()

    fun feeds(): List<String> = prefs.getStringSet(FEEDS, emptySet()).orEmpty().sorted()

    fun addFeed(url: String) {
        prefs.edit().putStringSet(FEEDS, feeds().toSet() + url.trim()).apply()
    }

    fun removeFeed(url: String) {
        prefs.edit().putStringSet(FEEDS, feeds().toSet() - url).apply()
    }

    fun position(id: String): Long = prefs.getLong("position:$id", 0)

    fun savePosition(id: String, ms: Long) {
        prefs.edit().putLong("position:$id", ms).apply()
    }

    /** presentation.json is written last, so its presence means every file arrived. */
    fun downloaded(): List<LocalEpisode> =
        root.listFiles().orEmpty().mapNotNull { dir ->
            val json = File(dir, "presentation.json").takeIf { it.exists() } ?: return@mapNotNull null
            runCatching { LocalEpisode(dir, Presentation.parse(json.readText())) }.getOrNull()
        }

    suspend fun fetchFeed(url: String): List<RemoteEpisode> = withContext(Dispatchers.IO) {
        parseRss(get(url))
    }

    suspend fun download(episode: RemoteEpisode, onProgress: (Float) -> Unit): LocalEpisode =
        withContext(Dispatchers.IO) {
            val json = get(episode.presentationUrl)
            val presentation = Presentation.parse(json)
            val base = episode.presentationUrl.substringBeforeLast('/')
            val dir = File(root, presentation.id.replace(Regex("[^A-Za-z0-9._-]"), "_"))
            dir.mkdirs()
            val files = presentation.files()
            files.forEachIndexed { index, relative ->
                val target = File(dir, relative)
                if (!target.exists()) {
                    target.parentFile?.mkdirs()
                    val partial = File(target.path + ".part")
                    http.newCall(Request.Builder().url("$base/$relative").build()).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $relative")
                        partial.outputStream().use { out -> response.body!!.byteStream().copyTo(out) }
                    }
                    partial.renameTo(target)
                }
                onProgress((index + 1f) / files.size)
            }
            File(dir, "presentation.json").writeText(json)
            LocalEpisode(dir, presentation)
        }

    fun delete(episode: LocalEpisode) {
        episode.dir.deleteRecursively()
        prefs.edit().remove("position:${episode.id}").apply()
    }

    private fun get(url: String): String =
        http.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
            response.body!!.string()
        }

    private fun parseRss(xml: String): List<RemoteEpisode> {
        val parser = Xml.newPullParser().apply { setInput(xml.reader()) }
        val episodes = mutableListOf<RemoteEpisode>()
        var title = ""
        var guid = ""
        var presentation: String? = null
        var inItem = false
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "item" -> { inItem = true; title = ""; guid = ""; presentation = null }
                    "title" -> if (inItem) title = parser.nextText()
                    "guid" -> if (inItem) guid = parser.nextText()
                    "presentation" -> if (parser.namespace == PAGECAST_NS) presentation = parser.getAttributeValue(null, "url")
                }
                XmlPullParser.END_TAG -> if (parser.name == "item") {
                    inItem = false
                    // Items without a presentation are plain podcast episodes, which Pagecast can't show yet.
                    presentation?.let { episodes += RemoteEpisode(guid.ifEmpty { it }, title, it) }
                }
            }
        }
        return episodes
    }

    private companion object {
        const val FEEDS = "feeds"
        // Must match PAGECAST_NS in the pipeline's feed.py.
        const val PAGECAST_NS = "https://github.com/nahueltaibo/article-to-podcast/ns/1"
    }
}
