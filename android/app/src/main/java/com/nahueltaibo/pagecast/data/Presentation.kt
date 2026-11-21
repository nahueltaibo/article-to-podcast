package com.nahueltaibo.pagecast.data

import org.json.JSONArray
import org.json.JSONObject

/** One narrated article, as described by the pipeline's presentation.json. Paths are relative to the episode. */
data class Presentation(
    val id: String,
    val title: String,
    val site: String,
    val articleUrl: String,
    val durationMs: Long,
    val audioFile: String,
    val narrationFile: String?,
    val chimeFile: String?,
    val cover: String?,
    val segments: List<Segment>,
    val media: Map<String, Media>,
    val chapters: List<Chapter>,
) {
    data class Segment(val startMs: Long, val media: String?)
    data class Media(val file: String?, val animatedFile: String?, val caption: String?)
    data class Chapter(val startMs: Long, val title: String, val image: String?)

    /** Older episodes have only audio.mp3, with the chime mixed in. */
    val playsOwnChime get() = narrationFile != null && chimeFile != null

    val playbackFile get() = if (playsOwnChime) narrationFile!! else audioFile

    /** Where the chime belongs: the start of every segment that brings new media. */
    val chimeAtMs get() = if (playsOwnChime) segments.filter { it.media != null }.map { it.startMs } else emptyList()

    /** Every file the episode needs offline. */
    fun files(): List<String> =
        (listOf(playbackFile) + listOfNotNull(chimeFile.takeIf { playsOwnChime }, cover) +
            media.values.flatMap { listOfNotNull(it.file, it.animatedFile) } +
            chapters.mapNotNull { it.image }).distinct()

    /** The image on screen at [positionMs]: the latest media reached so far, animated when available. */
    fun imageAt(positionMs: Long): String? {
        var image = cover
        for (segment in segments) {
            if (segment.startMs > positionMs) break
            val m = segment.media?.let { media[it] } ?: continue
            image = m.animatedFile ?: m.file ?: image
        }
        return image
    }

    fun chapterIndexAt(positionMs: Long): Int = chapters.indexOfLast { it.startMs <= positionMs }

    companion object {
        fun parse(json: String): Presentation {
            val root = JSONObject(json)
            val article = root.getJSONObject("article")
            val audio = root.getJSONObject("audio")
            return Presentation(
                id = root.getString("id"),
                title = article.getString("title"),
                site = article.optString("site"),
                articleUrl = article.getString("url"),
                durationMs = audio.getLong("durationMs"),
                audioFile = audio.getString("file"),
                narrationFile = audio.optNullableString("narrationFile"),
                chimeFile = audio.optNullableString("chimeFile"),
                cover = root.optNullableString("cover"),
                segments = root.getJSONArray("segments").objects().map {
                    Segment(it.getLong("startMs"), it.optNullableString("media"))
                },
                media = root.getJSONArray("media").objects().associate {
                    it.getString("id") to Media(
                        it.optNullableString("file"),
                        it.optNullableString("animatedFile"),
                        it.optNullableString("caption"),
                    )
                },
                chapters = root.optJSONArray("chapters")?.objects()?.map {
                    Chapter(it.getLong("startMs"), it.getString("title"), it.optNullableString("image"))
                }.orEmpty(),
            )
        }

        private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }

        private fun JSONObject.optNullableString(key: String): String? =
            if (isNull(key)) null else optString(key).ifEmpty { null }
    }
}
