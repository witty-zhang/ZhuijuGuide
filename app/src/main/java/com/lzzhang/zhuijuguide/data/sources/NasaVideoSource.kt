package com.lzzhang.zhuijuguide.data.sources

import com.lzzhang.zhuijuguide.data.Http
import com.lzzhang.zhuijuguide.model.Episode
import com.lzzhang.zhuijuguide.model.WorkSummary
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder

/**
 * NASA 影像库：公共领域的官方影像，提供搜索接口和直连 MP4，适合用来验证整条播放链路。
 */
class NasaVideoSource(
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) : SourceProvider {

    override val id = "nasa"
    override val name = "NASA 影像库"
    override val tagline = "公共领域的官方航天影像，可搜索、原生播放"

    override suspend fun featured(): List<WorkSummary> {
        return parseItems(Http.getText(searchUrl(FEATURED_QUERY, PAGE_SIZE)))
    }

    override suspend fun search(query: String): List<WorkSummary> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        return parseItems(Http.getText(searchUrl(needle, PAGE_SIZE)))
    }

    override suspend fun episodes(work: WorkSummary): List<Episode> {
        val encoded = URLEncoder.encode(work.itemId, "UTF-8").replace("+", "%20")
        val payload = Http.getText("$ASSET_BASE/video/$encoded/collection.json")
        val files = json.parseToJsonElement(payload).jsonArray
            .mapNotNull { it.jsonPrimitive.contentOrNull }
            .filter { it.endsWith(".mp4", ignoreCase = true) }

        fun pick(tag: String) = files.firstOrNull { it.contains("~$tag.mp4") }?.toPlayableUrl()

        return buildList {
            pick("mobile")?.let { add(Episode("流畅", it)) }
            pick("medium")?.let { add(Episode("高清", it)) }
            pick("large")?.let { add(Episode("超清", it)) }
            pick("orig")?.let { add(Episode("原画", it)) }
            if (isEmpty()) files.firstOrNull()?.toPlayableUrl()?.let { add(Episode("播放", it)) }
        }
    }

    private fun searchUrl(query: String, pageSize: Int): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return "$API_BASE/search?q=$encoded&media_type=video&page_size=$pageSize"
    }

    private fun parseItems(payload: String): List<WorkSummary> {
        val items = runCatching {
            val root = json.parseToJsonElement(payload).jsonObject
            root["collection"]?.jsonObject?.get("items")?.jsonArray
        }.getOrNull() ?: return emptyList()
        return items.mapNotNull { element -> parseItem(element) }
    }

    private fun parseItem(element: JsonElement): WorkSummary? = runCatching {
        val entry = element as? JsonObject ?: return@runCatching null
        val data = entry["data"]?.jsonArray?.firstOrNull() as? JsonObject ?: return@runCatching null
        val itemId = data["nasa_id"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
        val title = data["title"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (title.isEmpty()) return@runCatching null
        val description = data["description"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        val date = data["date_created"]?.jsonPrimitive?.contentOrNull?.take(10)
        val poster = entry["links"]?.jsonArray?.firstNotNullOfOrNull { link ->
            (link as? JsonObject)?.get("href")?.jsonPrimitive?.contentOrNull
        }
        return WorkSummary(
            id = "$id:$itemId",
            sourceId = id,
            sourceName = name,
            itemId = itemId,
            title = title,
            description = description,
            posterUrl = poster?.toHttps(),
            dateText = date,
        )
    }.getOrNull()

    private fun String.toHttps(): String = replaceFirst("http://", "https://")

    /** NASA 的文件名里常有空格，必须按路径段编码后才能交给播放器。 */
    private fun String.toPlayableUrl(): String {
        val https = toHttps()
        val schemeEnd = https.indexOf("://")
        if (schemeEnd < 0) return https
        val pathStart = https.indexOf('/', schemeEnd + 3)
        if (pathStart < 0) return https
        val origin = https.substring(0, pathStart)
        val path = https.substring(pathStart + 1)
        val encodedPath = path.split('/').joinToString("/") { segment ->
            URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
        }
        return "$origin/$encodedPath"
    }

    private companion object {
        const val API_BASE = "https://images-api.nasa.gov"
        const val ASSET_BASE = "https://images-assets.nasa.gov"
        const val FEATURED_QUERY = "artemis"
        const val PAGE_SIZE = 30
    }
}
