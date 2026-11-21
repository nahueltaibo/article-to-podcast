package com.nahueltaibo.pagecast.data

import android.content.Context

/** Listener preferences. Read on every use, so changes apply mid-episode. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var chime: Boolean
        get() = prefs.getBoolean(CHIME, true)
        set(value) = prefs.edit().putBoolean(CHIME, value).apply()

    private companion object {
        const val CHIME = "chime"
    }
}
