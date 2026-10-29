package com.lzzhang.zhuijuguide.data

import android.content.Context

class GuidePreferences(context: Context) {
    private val preferences = context.getSharedPreferences("zhuiju_guide", Context.MODE_PRIVATE)

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
    }
}
