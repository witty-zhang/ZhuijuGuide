package com.lzzhang.zhuijuguide.data

import com.lzzhang.zhuijuguide.model.MediaResource
import com.lzzhang.zhuijuguide.model.PlaylistEntry
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
                category = entry.group?.ifBlank { "我的直播" } ?: "我的直播",
                type = ResourceType.VIDEO,
                availability = Availability.UNKNOWN,
                riskLevel = RiskLevel.UNKNOWN,
                rating = null,
                lastChecked = null,
                tags = listOf("M3U", "本地导入"),
                sourceLabel = "本地播放列表",
                isImported = true,
            )
        }
    }

    private fun parseExtInf(line: String, extendedGroup: String?): PlaylistEntry {
        val metadata = line.substringBefore(',', "")
        val title = line.substringAfter(',', "").trim()
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

    private fun isHttpUrl(value: String): Boolean {
        return value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
    }

    private companion object {
        val ATTRIBUTE_REGEX = Regex("""([\w-]+)\s*=\s*"([^"]*)"""")
    }
}
