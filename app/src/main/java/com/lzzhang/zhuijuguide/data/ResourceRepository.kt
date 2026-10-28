package com.lzzhang.zhuijuguide.data

import android.content.Context
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

    suspend fun loadResources(): List<MediaResource> = withContext(Dispatchers.IO) {
        loadBundledResources()
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

    private fun loadBundledResources(): List<MediaResource> {
        val text = context.assets.open("starter_resources.json")
            .bufferedReader()
            .use { it.readText() }
        return parser.parse(text)
    }
}
