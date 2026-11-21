package com.nahueltaibo.pagecast.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun readsEachStoredName() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.from(it.name)) }
    }

    @Test
    fun missingOrUnknownFollowsTheSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.from(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.from("sepia"))
    }
}
