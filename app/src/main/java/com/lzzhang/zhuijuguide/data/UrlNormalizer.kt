package com.lzzhang.zhuijuguide.data

import java.net.IDN

/**
 * 中文域名（IDN）要先转成 punycode 才能解析出主机名，
 * 否则 HttpURLConnection 会直接报 DNS 失败。
 */
object UrlNormalizer {
    private val AUTHORITY_PATTERN = Regex("""^(https?://)([^/?#]+)(.*)$""", RegexOption.IGNORE_CASE)

    fun toAscii(address: String): String {
        val match = AUTHORITY_PATTERN.find(address) ?: return address
        val authority = match.groupValues[2]
        if (authority.all { it.code < 0x80 }) return address
        val host = authority.substringBefore(':')
        val port = authority.substring(host.length)
        val asciiHost = runCatching { IDN.toASCII(host) }.getOrDefault(host)
        return match.groupValues[1] + asciiHost + port + match.groupValues[3]
    }
}
