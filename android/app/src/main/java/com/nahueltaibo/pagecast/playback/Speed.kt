package com.nahueltaibo.pagecast.playback

import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Playback speed rules, in 0.1 steps like Pocket Casts. Rounding keeps repeated steps from drifting. */
object Speed {
    const val MIN = 0.5f
    const val MAX = 3.0f
    const val STEP = 0.1f
    val PRESETS = listOf(0.8f, 1.0f, 1.2f, 1.5f, 1.8f, 2.0f)

    fun clamp(speed: Float): Float = ((speed * 10).roundToInt() / 10f).coerceIn(MIN, MAX)

    fun step(speed: Float, steps: Int): Float = clamp(speed + steps * STEP)

    fun label(speed: Float): String = "%.1f×".format(Locale.ROOT, clamp(speed))

    /** Wall-clock time left, since a minute of audio at 2× takes 30 seconds. */
    fun remainingMs(durationMs: Long, positionMs: Long, speed: Float): Long =
        ((durationMs - positionMs).coerceAtLeast(0) / clamp(speed).toDouble()).roundToLong()
}
