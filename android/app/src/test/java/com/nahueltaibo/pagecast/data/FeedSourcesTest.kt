package com.nahueltaibo.pagecast.data

import com.nahueltaibo.pagecast.data.FeedSources.DEFAULT
import com.nahueltaibo.pagecast.data.FeedSources.LEGACY
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedSourcesTest {
    private val other = "https://example.com/feed.xml"

    @Test fun freshInstallGetsDefault() =
        assertEquals(setOf(DEFAULT), FeedSources.seed(emptySet(), alreadySeeded = false, optedOut = false))

    @Test fun keepsListenerFeedsWhenSeeding() =
        assertEquals(setOf(other, DEFAULT), FeedSources.seed(setOf(other), alreadySeeded = false, optedOut = false))

    @Test fun removedDefaultStaysRemoved() =
        assertEquals(setOf(other), FeedSources.seed(setOf(other), alreadySeeded = true, optedOut = false))

    @Test fun oldSwitchOffSkipsDefault() =
        assertEquals(emptySet<String>(), FeedSources.seed(emptySet(), alreadySeeded = false, optedOut = true))

    @Test fun legacyUrlMovesToDefault() =
        assertEquals(setOf(DEFAULT, other), FeedSources.seed(setOf(LEGACY, other), alreadySeeded = true, optedOut = true))
}
