package com.nahueltaibo.pagecast.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedTest {
    @Test
    fun stepsStayOnTenthsAndWithinRange() {
        var speed = 1.0f
        repeat(7) { speed = Speed.step(speed, 1) }
        assertEquals(1.7f, speed)
        assertEquals(Speed.MAX, Speed.step(2.95f, 1))
        assertEquals(Speed.MIN, Speed.step(0.5f, -1))
    }

    @Test
    fun labelShowsOneDecimal() {
        assertEquals("1.0×", Speed.label(1f))
        assertEquals("1.5×", Speed.label(1.49f))
    }

    @Test
    fun remainingTimeShrinksWithSpeed() {
        assertEquals(30_000L, Speed.remainingMs(durationMs = 90_000, positionMs = 30_000, speed = 2f))
        assertEquals(0L, Speed.remainingMs(durationMs = 1_000, positionMs = 5_000, speed = 1f))
    }
}
