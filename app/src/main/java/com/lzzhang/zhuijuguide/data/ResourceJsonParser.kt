package com.lzzhang.zhuijuguide.data

import com.lzzhang.zhuijuguide.model.Availability
import com.lzzhang.zhuijuguide.model.MediaResource
import com.lzzhang.zhuijuguide.model.ResourceType
import com.lzzhang.zhuijuguide.model.RiskLevel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray

class ResourceJsonParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
) {
    fun parse(text: String): List<MediaResource> {
        val root = json.parseToJsonElement(text)
        val entries = findResourceArray(root)
        return entries.mapNotNull(::parseResource)
            .distinctBy { it.id }
    }

    private fun findResourceArray(root: JsonElement): List<JsonObject> {
        return when (root) {
            is JsonArray -> root.mapNotNull { it as? JsonObject }
            is JsonObject -> {
                val candidates = listOf("resources", "items", "data", "list")
                val nested = candidates.firstNotNullOfOrNull { key ->
                    when (val value = root[key]) {
                        is JsonArray -> value
                        is JsonObject -> findArrayInObject(value)
                        else -> null
                    }
                }
                nested?.mapNotNull { it as? JsonObject } ?: emptyList()
            }
            else -> emptyList()
        }
    }

    private fun findArrayInObject(value: JsonObject): JsonArray? {
        return listOf("resources", "items", "list", "data")
            .firstNotNullOfOrNull { key ->
                runCatching { value[key]?.jsonArray }.getOrNull()
            }
    }

    private fun parseResource(item: JsonObject): MediaResource? {
        val title = item.string("title", "name", "resource", "resource_name")
            ?.trim()
            .orEmpty()
        val url = item.link("url", "link", "homepage", "address", "href")
            ?.trim()
            .orEmpty()
        if (title.isBlank() || url.isBlank()) return null

        val description = item.string("description", "summary", "intro", "remark")
            .orEmpty()
            .trim()
        val category = item.string("category", "category_name", "group")
            ?: item.arrayText("categories", "topics", "tags")
            ?: "其他"
        val categories = category.split(',', '，', '/')
            .map(String::trim)
            .filter(String::isNotBlank)
        val primaryCategory = categories.firstOrNull() ?: "其他"
        val searchable = "$title $description $category".lowercase()

        val rawStatus = item.string("status", "availability", "state")
        val rawRisk = item.string("risk", "risk_level", "riskLevel")
        val rating = item.number("rating", "score", "recommendation", "recommend")
        val tags = buildList {
            item.arrayValues("tags", "topics", "categories").forEach { add(it) }
            item.string("source", "contributor")?.takeIf(String::isNotBlank)?.let(::add)
        }.distinct()
        val id = item.string("id", "slug", "key")
            ?: "${title}_${url}".hashCode().toUInt().toString(16)

        return MediaResource(
            id = id,
            title = title,
            description = description.ifBlank { "暂无简介" },
            url = url,
            category = primaryCategory,
            type = inferType(url, primaryCategory, searchable),
            availability = parseAvailability(rawStatus),
            riskLevel = parseRisk(rawRisk),
            rating = rating,
            lastChecked = item.string("last_checked", "lastChecked", "checked_at", "checkedAt", "date"),
            tags = tags,
            sourceLabel = item.string("sourceLabel", "source") ?: "远程资源",
        )
    }

    private fun inferType(url: String, category: String, searchable: String): ResourceType {
        val path = url.substringBefore('?').lowercase()
        return when {
            path.endsWith(".m3u8") || path.endsWith(".mp4") ||
                path.endsWith(".webm") || path.endsWith(".mkv") -> ResourceType.VIDEO
            path.endsWith(".m3u") -> ResourceType.PLAYLIST
            category.contains("开源") || searchable.contains("github") -> ResourceType.OPEN_SOURCE
            category.contains("app", ignoreCase = true) ||
                category.contains("应用") -> ResourceType.APP
            else -> ResourceType.WEB
        }
    }

    private fun parseAvailability(value: String?): Availability {
        val text = value.orEmpty().lowercase()
        return when {
            text.contains("offline") || text.contains("失效") || text.contains("无法访问") ||
                text.contains("unavailable") || text == "down" -> Availability.OFFLINE
            text.contains("restricted") || text.contains("受限") || text.contains("需梯子") ||
                text.contains("vip") -> Availability.RESTRICTED
            text.contains("available") || text.contains("online") || text.contains("可访问") ||
                text.contains("正常") || text == "ok" -> Availability.AVAILABLE
            else -> Availability.UNKNOWN
        }
    }

    private fun parseRisk(value: String?): RiskLevel {
        val text = value.orEmpty().lowercase()
        return when {
            text.contains("high") || text.contains("高") -> RiskLevel.HIGH
            text.contains("medium") || text.contains("中") -> RiskLevel.MEDIUM
            text.contains("low") || text.contains("低") -> RiskLevel.LOW
            else -> RiskLevel.UNKNOWN
        }
    }

    private fun JsonObject.string(vararg keys: String): String? {
        for (key in keys) {
            val primitive = this[key] as? JsonPrimitive ?: continue
            if (primitive.isString) return primitive.content
            primitive.content.takeIf { it != "null" }?.let { return it }
        }
        return null
    }

    private fun JsonObject.link(vararg keys: String): String? {
        for (key in keys) {
            val element = this[key] ?: continue
            val raw = when (element) {
                is JsonPrimitive -> element.content
                is JsonObject -> element.string("url", "href", "value")
                else -> null
            }
            if (!raw.isNullOrBlank()) return raw
        }
        return null
    }

    private fun JsonObject.number(vararg keys: String): Float? {
        for (key in keys) {
            val primitive = this[key] as? JsonPrimitive ?: continue
            primitive.doubleOrNull?.let { return it.toFloat() }
            primitive.content.toFloatOrNull()?.let { return it }
        }
        return null
    }

    private fun JsonObject.arrayValues(vararg keys: String): List<String> {
        for (key in keys) {
            val array = this[key] as? JsonArray ?: continue
            return array.mapNotNull { element ->
                when (element) {
                    is JsonPrimitive -> element.content.takeIf(String::isNotBlank)
                    is JsonObject -> element.string("name", "title", "label")
                    else -> null
                }
            }
        }
        return emptyList()
    }

    private fun JsonObject.arrayText(vararg keys: String): String? {
        return arrayValues(*keys).takeIf(List<String>::isNotEmpty)?.joinToString(" / ")
    }
}
