package com.nahueltaibo.pagecast.data

import android.content.Context

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        /** An unknown or missing value follows the phone. */
        fun from(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/** Listener preferences. Read on every use, so changes apply mid-episode. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var chime: Boolean
        get() = prefs.getBoolean(CHIME, true)
        set(value) = prefs.edit().putBoolean(CHIME, value).apply()

    /** On by default, so a fresh install has something to listen to. */
    var recommendedFeed: Boolean
        get() = prefs.getBoolean(RECOMMENDED_FEED, true)
        set(value) = prefs.edit().putBoolean(RECOMMENDED_FEED, value).apply()

    /** One speed for every episode, the default in AntennaPod and Pocket Casts. */
    var speed: Float
        get() = prefs.getFloat(SPEED, 1f)
        set(value) = prefs.edit().putFloat(SPEED, value).apply()

    var theme: ThemeMode
        get() = ThemeMode.from(prefs.getString(THEME, null))
        set(value) = prefs.edit().putString(THEME, value.name).apply()

    private companion object {
        const val THEME = "theme"
        const val SPEED = "speed"
        const val CHIME = "chime"
        const val RECOMMENDED_FEED = "recommendedFeed"
    }
}
