package com.lzzhang.zhuijuguide.data

import android.content.Context
import com.lzzhang.zhuijuguide.model.Availability
import com.lzzhang.zhuijuguide.model.MediaResource
import com.lzzhang.zhuijuguide.model.ResourceCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ResourceRepository(
    private val context: Context,
    private val parser: ResourceJsonParser = ResourceJsonParser(),
    private val m3uParser: M3uParser = M3uParser(),
    private val livePlaylistParser: LivePlaylistParser = LivePlaylistParser(m3uParser),
    private val liveClient: LiveSourceClient = LiveSourceClient(context, livePlaylistParser),
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

    /** 内置的直播源订阅地址，地址里的状态同样以本机检测为准。 */
    suspend fun loadLiveSources(): List<MediaResource> = withContext(Dispatchers.IO) {
        val text = context.assets.open("starter_live_sources.json")
            .bufferedReader()
            .use { it.readText() }
        parser.parse(text).map { resource ->
            resource.copy(
                category = ResourceCategory.LIVE,
                availability = Availability.UNKNOWN,
                lastChecked = null,
                sourceLabel = "直播源订阅地址",
            )
        }
    }

    /** 下载一个直播源并展开成频道；TXT 与 M3U 两种写法都支持。 */
    suspend fun fetchLiveChannels(source: MediaResource): List<MediaResource> = withContext(Dispatchers.IO) {
        val content = liveClient.download(source.url)
        val entries = livePlaylistParser.parse(content)
        check(entries.isNotEmpty()) {
            "没有从这份直播源里读出频道，可能它不是 TXT / M3U 频道列表"
        }
        m3uParser.toResources(
            entries = entries,
            category = ResourceCategory.LIVE,
            baseTags = listOf("直播源"),
            sourceLabel = source.title,
            isImported = false,
        )
    }

    /**
     * 支持 .m3u 和 .m3u8 两种后缀：只要文件里是「频道名 + 流地址」的列表就能导入。
     * 整份文件就是一路 HLS 清单（分片是相对路径）时，本机没有对应的分片文件，直接说明原因。
     */
    suspend fun importPlaylist(content: String): List<MediaResource> = withContext(Dispatchers.IO) {
        val resources = m3uParser.toResources(content)
        check(resources.isNotEmpty()) {
            if (m3uParser.isHlsManifest(content)) {
                "这是单路 HLS 清单，不是频道列表；它的分片路径是相对的，在本机无法直接播放"
            } else {
                "未在文件中发现有效的频道地址"
            }
        }
        playlistFile.writeText(content)
        resources
    }
}
