package com.lzzhang.zhuijuguide.data.sources

import com.lzzhang.zhuijuguide.data.Http
import com.lzzhang.zhuijuguide.model.Episode
import com.lzzhang.zhuijuguide.model.WorkSummary
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * RSS / 播客订阅源：发布者在 feed 里用 <enclosure> 主动提供音频或视频地址，
 * 这里只做标准的订阅解析，点开后用 Media3 原生播放。
 */
class RssFeedSource(val feedUrl: String) : SourceProvider {

    override val id: String = "rss-" + feedUrl.hashCode().toUInt().toString(16)

    private var feedTitle: String? = null

    override val name: String
        get() = feedTitle ?: runCatching { URL(feedUrl).host }.getOrNull() ?: "RSS 订阅"

    override val tagline: String
        get() = "RSS / 播客订阅 · $feedUrl"

    private var cached: List<Pair<WorkSummary, Episode>> = emptyList()
    private var cachedAt = 0L

    override suspend fun featured(): List<WorkSummary> = entries().map { it.first }

    override suspend fun search(query: String): List<WorkSummary> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        return entries()
            .map { it.first }
            .filter { work ->
                work.title.contains(needle, ignoreCase = true) ||
                    work.description.contains(needle, ignoreCase = true)
            }
    }

    override suspend fun episodes(work: WorkSummary): List<Episode> {
        entries().firstOrNull { it.first.id == work.id }?.let { return listOf(it.second) }
        // 缓存过期或进程重建时，重新拉一次订阅再匹配
        cachedAt = 0L
        return entries().firstOrNull { it.first.id == work.id }?.let { listOf(it.second) } ?: emptyList()
    }

    private suspend fun entries(): List<Pair<WorkSummary, Episode>> {
        val fresh = System.currentTimeMillis() - cachedAt < CACHE_TTL_MS
        if (cached.isNotEmpty() && fresh) return cached

        val feed = RssParser.parse(Http.getText(feedUrl, MAX_FEED_BYTES))
        feedTitle = feed.title?.takeIf { it.isNotBlank() }
        val parsed = feed.items.mapNotNull { item ->
            val mediaUrl = item.mediaUrl?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val key = item.guid?.takeIf { it.isNotBlank() } ?: mediaUrl
            val work = WorkSummary(
                id = "$id:$key",
                sourceId = id,
                sourceName = name,
                itemId = key,
                title = item.title.ifBlank { "未命名节目" },
                description = RssParser.cleanHtml(item.description),
                posterUrl = item.imageUrl ?: feed.imageUrl,
                dateText = RssParser.formatDate(item.pubDate),
            )
            val label = when {
                item.mediaType?.startsWith("audio", ignoreCase = true) == true -> "音频"
                item.mediaType?.startsWith("video", ignoreCase = true) == true -> "视频"
                else -> "播放"
            }
            work to Episode(label, mediaUrl)
        }
        cached = parsed
        cachedAt = System.currentTimeMillis()
        return parsed
    }

    private companion object {
        const val CACHE_TTL_MS = 5 * 60 * 1000L
        const val MAX_FEED_BYTES = 16 * 1024 * 1024
    }
}

internal data class RssItem(
    val title: String = "",
    val description: String = "",
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val imageUrl: String? = null,
    val pubDate: String? = null,
    val guid: String? = null,
)

internal data class RssFeed(
    val title: String? = null,
    val imageUrl: String? = null,
    val items: List<RssItem> = emptyList(),
)

internal object RssParser {

    fun parse(xml: String): RssFeed {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))

        var feedTitle: String? = null
        var feedImage: String? = null

        var itemTitle = ""
        var itemDescription = ""
        var itemMedia: String? = null
        var itemMediaType: String? = null
        var itemImage: String? = null
        var itemDate: String? = null
        var itemGuid: String? = null
        var inItem = false
        var inImage = false

        val text = StringBuilder()
        val items = mutableListOf<RssItem>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    // 关掉命名空间后标签名带前缀：itunes:image 要单独识别，其余只用本地名
                    val full = parser.name
                    val tag = if (full.startsWith("itunes:", ignoreCase = true)) {
                        "itunes_image"
                    } else {
                        full.substringAfter(':').lowercase()
                    }
                    text.clear()
                    when (tag) {
                        "item", "entry" -> {
                            inItem = true
                            itemTitle = ""
                            itemDescription = ""
                            itemMedia = null
                            itemMediaType = null
                            itemImage = null
                            itemDate = null
                            itemGuid = null
                        }

                        "image" -> if (!inItem) inImage = true
                        "enclosure" -> if (inItem) {
                            val url = parser.getAttributeValue(null, "url")
                            if (url != null && itemMedia == null) {
                                itemMedia = url
                                itemMediaType = parser.getAttributeValue(null, "type")
                            }
                        }

                        "thumbnail" -> if (inItem) {
                            itemImage = itemImage ?: parser.getAttributeValue(null, "url")
                        }

                        "content", "group" -> if (inItem) {
                            val url = parser.getAttributeValue(null, "url")
                            val type = parser.getAttributeValue(null, "type").orEmpty()
                            if (url != null && !type.startsWith("image") && itemMedia == null) {
                                itemMedia = url
                                itemMediaType = type.ifBlank { null }
                            }
                        }

                        "itunes_image", "logo" -> {
                            val href = parser.getAttributeValue(null, "href")
                            if (href != null) {
                                if (inItem) itemImage = itemImage ?: href else feedImage = feedImage ?: href
                            }
                        }
                    }
                }

                XmlPullParser.TEXT -> text.append(parser.text)

                XmlPullParser.END_TAG -> {
                    val full = parser.name
                    val tag = if (full.startsWith("itunes:", ignoreCase = true)) {
                        "itunes_image"
                    } else {
                        full.substringAfter(':').lowercase()
                    }
                    when (tag) {
                        "title" -> when {
                            inItem -> itemTitle = text.toString().trim()
                            inImage -> Unit
                            else -> feedTitle = text.toString().trim()
                        }
                        "description", "summary", "encoded" -> if (inItem) {
                            itemDescription = text.toString().trim()
                        }

                        "pubdate", "published", "updated" -> if (inItem) itemDate = text.toString().trim()
                        "guid", "id" -> if (inItem) itemGuid = text.toString().trim()
                        "url" -> if (inImage) feedImage = feedImage ?: text.toString().trim()
                        "image" -> if (!inItem) inImage = false
                        "item", "entry" -> {
                            inItem = false
                            if (itemTitle.isNotBlank() || itemMedia != null) {
                                items += RssItem(
                                    title = itemTitle,
                                    description = itemDescription,
                                    mediaUrl = itemMedia,
                                    mediaType = itemMediaType,
                                    imageUrl = itemImage,
                                    pubDate = itemDate,
                                    guid = itemGuid,
                                )
                            }
                        }
                    }
                    text.clear()
                }
            }
            event = parser.next()
        }
        return RssFeed(feedTitle, feedImage, items)
    }

    fun cleanHtml(raw: String): String {
        if (raw.isBlank()) return ""
        val withoutTags = raw.replace(Regex("<[^>]+>"), " ")
        return withoutTags
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun formatDate(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        runCatching {
            val parser = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            val date = parser.parse(raw) ?: return@runCatching null
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        }.getOrNull()?.let { return it }
        runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = parser.parse(raw.take(19)) ?: return@runCatching null
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        }.getOrNull()?.let { return it }
        return null
    }
}
