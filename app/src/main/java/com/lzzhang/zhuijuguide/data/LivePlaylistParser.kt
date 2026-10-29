package com.lzzhang.zhuijuguide.data

import com.lzzhang.zhuijuguide.model.PlaylistEntry
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * 直播源文件有两种常见写法：
 * 1. M3U / M3U8 频道列表（#EXTINF + 地址），交给 [M3uParser]；
 * 2. TXT 频道列表，一行一条「频道名,地址」，分组用「分组名,#genre#」标记。
 */
class LivePlaylistParser(private val m3uParser: M3uParser = M3uParser()) {
    fun parse(content: String): List<PlaylistEntry> {
        val text = content.removePrefix("\uFEFF")
        return if (text.contains("#EXTINF", ignoreCase = true)) {
            m3uParser.parse(text)
        } else {
            parseTxt(text)
        }
    }

    /** 直播源文件常见 UTF-8 与 GBK 两种编码，先按 UTF-8 严格解码，失败再退回 GB18030。 */
    fun decode(bytes: ByteArray): String {
        val utf8 = runCatching { strictDecode(bytes, Charsets.UTF_8) }
        if (utf8.isSuccess) return utf8.getOrThrow()
        return runCatching { Charset.forName("GB18030").decode(ByteBuffer.wrap(bytes)).toString() }
            .getOrDefault(String(bytes, Charsets.UTF_8))
    }

    private fun strictDecode(bytes: ByteArray, charset: Charset): String {
        val decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (error: CharacterCodingException) {
            throw IllegalArgumentException("内容不是合法的 ${charset.name()} 文本", error)
        }
    }

    private fun parseTxt(content: String): List<PlaylistEntry> {
        val entries = mutableListOf<PlaylistEntry>()
        var group: String? = null
        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim().removePrefix("\uFEFF")
            if (line.isBlank() || line.startsWith("#")) return@forEach
            // 频道名里可能带逗号，地址取最后一个逗号之后的部分
            val comma = line.lastIndexOf(',')
            if (comma <= 0) return@forEach
            val name = line.substring(0, comma).trim()
            val target = line.substring(comma + 1).trim()
            when {
                target == "#genre#" -> group = name.ifBlank { null }
                target.startsWith("http://", ignoreCase = true) ||
                    target.startsWith("https://", ignoreCase = true) ->
                    entries += PlaylistEntry(
                        title = name.ifBlank { "未命名频道" },
                        url = target,
                        group = group,
                    )
            }
        }
        return entries.distinctBy { it.url }
    }
}
