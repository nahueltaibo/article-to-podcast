package com.nahueltaibo.pagecast.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PresentationTest {
    private val json = """
        {
          "id": "ep", "cover": "cover.jpg",
          "article": {"url": "https://x.test/post", "title": "Post", "site": "x.test"},
          "audio": {"file": "audio.mp3", "durationMs": 9000},
          "segments": [
            {"id": "s1", "startMs": 0},
            {"id": "s2", "startMs": 2000, "media": "m1"},
            {"id": "s3", "startMs": 5000},
            {"id": "s4", "startMs": 7000, "media": "m2"}
          ],
          "media": [
            {"id": "m1", "file": "media/m1.jpg", "caption": null},
            {"id": "m2", "file": "media/m2.jpg", "animatedFile": "media/m2.gif"}
          ],
          "chapters": [
            {"startMs": 0, "title": "Post", "image": "cover.jpg"},
            {"startMs": 2000, "title": "Chart", "image": "media/m1.jpg"},
            {"startMs": 7000, "title": "Animation", "image": "media/m2.jpg"}
          ]
        }
    """.trimIndent()

    private val presentation = Presentation.parse(json)

    @Test
    fun imageFollowsTheLatestMediaAndPrefersAnimations() {
        assertEquals("cover.jpg", presentation.imageAt(1999))
        assertEquals("media/m1.jpg", presentation.imageAt(2000))
        assertEquals("media/m1.jpg", presentation.imageAt(6000))
        assertEquals("media/m2.gif", presentation.imageAt(7000))
    }

    @Test
    fun chapterIndexMatchesPosition() {
        assertEquals(0, presentation.chapterIndexAt(0))
        assertEquals(1, presentation.chapterIndexAt(4000))
        assertEquals(2, presentation.chapterIndexAt(8999))
    }

    @Test
    fun filesListsEverythingNeededOfflineOnce() {
        assertEquals(
            listOf("audio.mp3", "cover.jpg", "media/m1.jpg", "media/m2.jpg", "media/m2.gif"),
            presentation.files(),
        )
    }
}
