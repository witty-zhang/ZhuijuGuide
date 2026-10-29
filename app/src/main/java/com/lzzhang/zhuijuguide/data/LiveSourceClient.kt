package com.lzzhang.zhuijuguide.data

import android.content.Context
import android.webkit.WebSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 下载一份直播源清单（TXT 或 M3U）。User-Agent 与 APP 内网页一致，
 * 中文域名先转 punycode，遇到 3xx 跟随跳转。
 */
class LiveSourceClient(
    context: Context,
    private val decoder: LivePlaylistParser = LivePlaylistParser(),
) {
    private val userAgent: String = WebSettings.getDefaultUserAgent(context.applicationContext)

    suspend fun download(address: String): String = withContext(Dispatchers.IO) {
        val target = UrlNormalizer.toAscii(address)
        val connection = (URL(target).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "text/plain,application/x-mpegurl,*/*")
        }

        try {
            val status = connection.responseCode
            check(status in 200..299) { "直播源返回 HTTP $status" }
            val bytes = connection.inputStream.use(::readBounded)
            check(bytes.isNotEmpty()) { "直播源内容是空的" }
            decoder.decode(bytes)
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    private fun readBounded(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
            check(output.size() <= MAX_BYTES) { "直播源超过 8 MB，暂不支持" }
        }
        return output.toByteArray()
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val MAX_BYTES = 8 * 1024 * 1024
    }
}
