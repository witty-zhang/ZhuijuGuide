package com.lzzhang.zhuijuguide.data

import android.content.Context
import com.lzzhang.zhuijuguide.model.WorkSummary
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class GuidePreferences(context: Context) {
    private val preferences = context.getSharedPreferences("zhuiju_guide", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun favoriteWorks(): List<WorkSummary> {
        val raw = preferences.getString(KEY_FAVORITES, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(WorkSummary.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    fun saveFavoriteWorks(works: List<WorkSummary>) {
        val raw = json.encodeToString(ListSerializer(WorkSummary.serializer()), works)
        preferences.edit().putString(KEY_FAVORITES, raw).apply()
    }

    /** 用户自己填写的 RSS / 播客订阅地址，按添加顺序保存。 */
    fun feedUrls(): List<String> {
        return preferences.getString(KEY_FEEDS, "")
            .orEmpty()
            .split('\n')
            .map(String::trim)
            .filter(String::isNotBlank)
    }

    fun saveFeedUrls(urls: List<String>) {
        preferences.edit().putString(KEY_FEEDS, urls.joinToString("\n")).apply()
    }

    private companion object {
        const val KEY_FAVORITES = "favorite_works"
        const val KEY_FEEDS = "feed_urls"
    }
}
