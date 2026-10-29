package com.lzzhang.zhuijuguide.data

import android.content.Context

class GuidePreferences(context: Context) {
    private val preferences = context.getSharedPreferences("zhuiju_guide", Context.MODE_PRIVATE)

    var feedUrl: String
        get() = preferences.getString(KEY_FEED_URL, "").orEmpty()
        set(value) {
            preferences.edit().putString(KEY_FEED_URL, value.trim()).apply()
        }

    fun favorites(): Set<String> {
        return preferences.getStringSet(KEY_FAVORITES, emptySet())
            .orEmpty()
            .toSet()
    }

    fun saveFavorites(ids: Set<String>) {
        preferences.edit().putStringSet(KEY_FAVORITES, ids).apply()
    }

    private companion object {
        const val KEY_FAVORITES = "favorite_ids"
        const val KEY_FEED_URL = "feed_url"
    }
}
