package com.lzzhang.zhuijuguide.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

internal object Http {
    private const val USER_AGENT = "ZhuijuGuide/0.5 (Android)"
    private const val DEFAULT_MAX_BYTES = 4 * 1024 * 1024

    suspend fun getText(url: String, maxBytes: Int = DEFAULT_MAX_BYTES): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 12_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json,text/plain,*/*")
            val code = connection.responseCode
            check(code in 200..299) { "服务器返回 HTTP $code" }
            val buffer = ByteArray(16 * 1024)
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    check(output.size() <= maxBytes) { "数据超过 ${maxBytes / 1024 / 1024} MB 限制" }
                }
            }
            output.toString(Charsets.UTF_8.name())
        } finally {
            connection.disconnect()
        }
    }
}
