package com.nahueltaibo.pagecast.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationLabelTest {
    @Test
    fun roundsToMinutesAndSplitsHours() {
        assertEquals("1 min", durationLabel(10_000))
        assertEquals("31 min", durationLabel(1_884_000))
        assertEquals("1 h", durationLabel(3_600_000))
        assertEquals("1 h 5 min", durationLabel(3_900_000))
    }
}
