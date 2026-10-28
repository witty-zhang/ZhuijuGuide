package com.lzzhang.zhuijuguide

import com.lzzhang.zhuijuguide.data.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Test

class M3uParserTest {
    private val parser = M3uParser()

    @Test
    fun parsesAttributesAndChannelUrl() {
        val entries = parser.parse(
            """
            #EXTM3U
            #EXTINF:-1 tvg-name="新闻频道" group-title="新闻",备用名称
            https://media.example/news.m3u8
            """.trimIndent(),
        )

        assertEquals(1, entries.size)
        assertEquals("新闻频道", entries.single().title)
        assertEquals("新闻", entries.single().group)
        assertEquals("https://media.example/news.m3u8", entries.single().url)
    }

    @Test
    fun ignoresLocalAndMalformedUrls() {
        val entries = parser.parse(
            """
            #EXTM3U
            #EXTINF:-1,本地文件
            /Users/example/video.m3u8
            #EXTINF:-1,无效协议
            ftp://example.com/video
            #EXTINF:-1,有效频道
            http://example.com/live.m3u8
            """.trimIndent(),
        )

        assertEquals(1, entries.size)
        assertEquals("有效频道", entries.single().title)
    }
}
