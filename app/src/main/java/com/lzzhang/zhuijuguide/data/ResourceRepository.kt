package com.lzzhang.zhuijuguide.data

import android.content.Context
import com.lzzhang.zhuijuguide.model.MediaResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ResourceRepository(
    private val context: Context,
    private val parser: ResourceJsonParser = ResourceJsonParser(),
    private val m3uParser: M3uParser = M3uParser(),
) {
    private val cacheFile = File(context.filesDir, "resource_feed_cache.json")
    private val playlistFile = File(context.filesDir, "imported_playlist.m3u")

    suspend fun loadResources(): List<MediaResource> = withContext(Dispatchers.IO) {
        val cached = cacheFile
            .takeIf(File::isFile)
            ?.readText()
            ?.let { runCatching { parser.parse(it) }.getOrNull() }
            .orEmpty()
        cached.ifEmpty { loadBundledResources() }
    }

    suspend fun loadImportedPlaylist(): List<MediaResource> = withContext(Dispatchers.IO) {
        if (!playlistFile.isFile) return@withContext emptyList()
        runCatching { m3uParser.toResources(playlistFile.readText()) }.getOrDefault(emptyList())
    }

    suspend fun refreshResources(feedUrl: String): List<MediaResource> = withContext(Dispatchers.IO) {
        val url = feedUrl.trim()
        require(url.startsWith("https://") || url.startsWith("http://")) {
            "请输入有效的 HTTP 或 HTTPS 数据源地址"
        }
        val body = download(url)
        val resources = parser.parse(body)
        check(resources.isNotEmpty()) { "数据源中没有可识别的资源" }
        cacheFile.writeText(body)
        resources
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

    private fun download(address: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "ZhuijuGuide/0.1")
            connection.setRequestProperty("Accept", "application/json,text/plain,*/*")
            val code = connection.responseCode
            check(code in 200..299) { "数据源请求失败：HTTP $code" }

            val bytes = connection.inputStream.use { input ->
                val buffer = ByteArray(16 * 1024)
                val output = ByteArrayOutputStream()
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    check(total <= MAX_FEED_BYTES) { "数据源文件超过 5 MB 限制" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            return bytes.toString(Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val MAX_FEED_BYTES = 5 * 1024 * 1024
    }
}
