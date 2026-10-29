package com.lzzhang.zhuijuguide.data

import android.content.Context
import android.webkit.WebSettings
import com.lzzhang.zhuijuguide.model.Availability
import com.lzzhang.zhuijuguide.model.MediaResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.HttpURLConnection
import java.net.URL

/**
 * 在本机发起 HTTP 请求检测站点能否打开。
 * User-Agent 取 WebView 的默认值，和 APP 内实际打开站点时保持一致。
 */
class AvailabilityChecker(context: Context) {
    private val userAgent: String = WebSettings.getDefaultUserAgent(context.applicationContext)

    suspend fun checkAll(
        resources: List<MediaResource>,
        onResult: suspend (id: String, availability: Availability) -> Unit,
    ) = coroutineScope {
        val semaphore = Semaphore(MAX_CONCURRENCY)
        resources.map { resource ->
            async(Dispatchers.IO) {
                val availability = semaphore.withPermit { check(resource.url) }
                onResult(resource.id, availability)
            }
        }.awaitAll()
        Unit
    }

    private fun check(address: String): Availability {
        // 本地文件（比如导入的 m3u8 清单）没有 HTTP 状态码可测，保持待检测
        if (!address.startsWith("http://", ignoreCase = true) &&
            !address.startsWith("https://", ignoreCase = true)
        ) {
            return Availability.UNKNOWN
        }
        val connection = runCatching {
            (URL(UrlNormalizer.toAscii(address)).openConnection() as HttpURLConnection)
        }.getOrNull() ?: return Availability.OFFLINE

        return try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", userAgent)
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*")
            connection.setRequestProperty("Range", "bytes=0-2048")
            AvailabilityRules.classify(connection.responseCode)
        } catch (error: Exception) {
            // 超时、DNS 失败、连接被重置，都按无法访问处理
            Availability.OFFLINE
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 6_000
        const val MAX_CONCURRENCY = 6
    }
}

object AvailabilityRules {
    fun classify(statusCode: Int): Availability = when {
        statusCode in 200..399 -> Availability.AVAILABLE
        // 需要登录、被拦截或限流：站点还在，但本机访问不到内容
        statusCode == 401 || statusCode == 403 || statusCode == 429 || statusCode == 451 ->
            Availability.RESTRICTED
        statusCode <= 0 -> Availability.UNKNOWN
        else -> Availability.OFFLINE
    }
}
