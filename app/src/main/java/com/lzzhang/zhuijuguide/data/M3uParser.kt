package com.lzzhang.zhuijuguide.data

import com.lzzhang.zhuijuguide.model.MediaResource
import com.lzzhang.zhuijuguide.model.PlaylistEntry
import com.lzzhang.zhuijuguide.model.ResourceCategory
import com.lzzhang.zhuijuguide.model.ResourceType
import com.lzzhang.zhuijuguide.model.Availability
import com.lzzhang.zhuijuguide.model.RiskLevel

class M3uParser {
    fun parse(content: String): List<PlaylistEntry> {
        val entries = mutableListOf<PlaylistEntry>()
        var pending: PlaylistEntry? = null
        var extendedGroup: String? = null

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim().removePrefix("\uFEFF")
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    pending = parseExtInf(line, extendedGroup)
                }
                line.startsWith("#EXTGRP:", ignoreCase = true) -> {
                    extendedGroup = line.substringAfter(':').trim().ifBlank { null }
                }
                line.isNotBlank() && !line.startsWith("#") -> {
                    val current = pending
                    if (isHttpUrl(line)) {
                        entries += PlaylistEntry(
                            title = current?.title?.ifBlank { "未命名频道" } ?: "未命名频道",
                            url = line,
                            group = current?.group ?: extendedGroup,
                        )
                    }
                    pending = null
                    extendedGroup = null
                }
            }
        }
        return entries.distinctBy { it.url }
    }

    fun toResources(content: String): List<MediaResource> {
        return parse(content).map { entry ->
            MediaResource(
                id = "playlist_${entry.url.hashCode().toUInt().toString(16)}",
                title = entry.title,
                description = "来自本地播放列表的直播频道",
                url = entry.url,
                // 首页只保留固定的两个分类，本地导入的频道统一归到「M3U / M3U8」
                category = ResourceCategory.LOCAL,
                type = ResourceType.VIDEO,
                availability = Availability.UNKNOWN,
                riskLevel = RiskLevel.UNKNOWN,
                rating = null,
                lastChecked = null,
                tags = buildList {
                    add("M3U8")
                    add("本地导入")
                    entry.group?.takeIf(String::isNotBlank)?.let(::add)
                },
                sourceLabel = "本地播放列表",
                isImported = true,
            )
        }
    }

    private fun parseExtInf(line: String, extendedGroup: String?): PlaylistEntry {
        val (metadata, title) = splitMetadataAndTitle(line)
        val attributes = ATTRIBUTE_REGEX.findAll(metadata)
            .associate { match ->
                match.groupValues[1].lowercase() to match.groupValues[2]
            }
        return PlaylistEntry(
            title = attributes["tvg-name"].orEmpty().ifBlank { title }.ifBlank { "未命名频道" },
            url = "",
            group = attributes["group-title"].orEmpty().ifBlank { extendedGroup } ?: extendedGroup,
            logoUrl = attributes["tvg-logo"],
        )
    }

    /**
     * #EXTINF 的时长和属性在前，频道名在后，分隔用的是第一个引号外的逗号。
     * 属性值里也可能带逗号（比如 group-title="电影, 电视剧"），不能直接 substringBefore。
     */
    private fun splitMetadataAndTitle(line: String): Pair<String, String> {
        var inQuotes = false
        line.forEachIndexed { index, char ->
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes ->
                    return line.substring(0, index) to line.substring(index + 1).trim()
            }
        }
        return line to ""
    }

    /** 整个文件就是一路 HLS 清单（#EXT-X- 系列标签），不是频道列表。 */
    fun isHlsManifest(content: String): Boolean {
        val lines = content.lineSequence()
            .map { it.trim().removePrefix("\uFEFF") }
            .filter(String::isNotBlank)
            .toList()
        return lines.any { it.startsWith("#EXTM3U", ignoreCase = true) } &&
            lines.any { it.startsWith("#EXT-X-", ignoreCase = true) }
    }

    private fun isHttpUrl(value: String): Boolean {
        return value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
    }

    private companion object {
        val ATTRIBUTE_REGEX = Regex("""([\w-]+)\s*=\s*"([^"]*)"""")
    }
}
