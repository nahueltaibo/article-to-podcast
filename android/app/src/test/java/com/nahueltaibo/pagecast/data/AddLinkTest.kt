package com.nahueltaibo.pagecast.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddLinkTest {
    @Test
    fun readsTheEncodedFeedUrl() {
        assertEquals(
            "http://192.168.1.10:8000/podcast.xml",
            feedFromAddLink("pagecast://add?feed=http%3A%2F%2F192.168.1.10%3A8000%2Fpodcast.xml"),
        )
    }

    @Test
    fun rejectsOtherLinksAndSchemes() {
        assertNull(feedFromAddLink("pagecast://play?feed=http%3A%2F%2Fx.test%2Fpodcast.xml"))
        assertNull(feedFromAddLink("https://add?feed=http%3A%2F%2Fx.test%2Fpodcast.xml"))
        assertNull(feedFromAddLink("pagecast://add?feed=file%3A%2F%2F%2Fsdcard%2Fx"))
        assertNull(feedFromAddLink("pagecast://add"))
    }
}
