package com.lzzhang.zhuijuguide.data

import android.content.Context
import com.lzzhang.zhuijuguide.model.Availability
import com.lzzhang.zhuijuguide.model.MediaResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ResourceRepository(
    private val context: Context,
    private val parser: ResourceJsonParser = ResourceJsonParser(),
    private val m3uParser: M3uParser = M3uParser(),
) {
    private val playlistFile = File(context.filesDir, "imported_playlist.m3u")

    /**
     * 读取内置的在线影视目录。目录里的 status / last_checked 是上游整理时的旧结果，
     * 这里一律清空，状态只由 [AvailabilityChecker] 在本机实测得出。
     */
    suspend fun loadResources(): List<MediaResource> = withContext(Dispatchers.IO) {
        val text = context.assets.open("starter_resources.json")
            .bufferedReader()
            .use { it.readText() }
        parser.parse(text).map { resource ->
            resource.copy(
                availability = Availability.UNKNOWN,
                lastChecked = null,
                sourceLabel = "在线影视目录 · CC BY 4.0",
            )
        }
    }

    suspend fun loadImportedPlaylist(): List<MediaResource> = withContext(Dispatchers.IO) {
        if (!playlistFile.isFile) return@withContext emptyList()
        runCatching { m3uParser.toResources(playlistFile.readText()) }.getOrDefault(emptyList())
    }

    suspend fun importPlaylist(content: String): List<MediaResource> = withContext(Dispatchers.IO) {
        val resources = m3uParser.toResources(content)
        check(resources.isNotEmpty()) { "未在文件中发现有效的 M3U 频道" }
        playlistFile.writeText(content)
        resources
    }
}
