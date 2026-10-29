package com.lzzhang.zhuijuguide

import com.lzzhang.zhuijuguide.data.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun parsesM3u8ChannelList() {
        val entries = parser.parse(
            """
            #EXTM3U
            #EXTINF:-1 tvg-id="cctv1" tvg-name="CCTV-1" group-title="央视",CCTV-1 综合
            https://media.example/cctv1.m3u8
            #EXTINF:-1 tvg-id="cctv2" tvg-name="CCTV-2" group-title="央视",CCTV-2 财经
            https://media.example/cctv2.m3u8
            """.trimIndent(),
        )

        assertEquals(2, entries.size)
        assertEquals("https://media.example/cctv2.m3u8", entries[1].url)
        assertEquals("央视", entries[1].group)
    }

    @Test
    fun keepsTitleWhenAttributesContainCommas() {
        val entries = parser.parse(
            """
            #EXTM3U
            #EXTINF:-1 group-title="电影, 电视剧",备用名, 带逗号
            https://media.example/movie.m3u8
            """.trimIndent(),
        )

        assertEquals("备用名, 带逗号", entries.single().title)
        assertEquals("电影, 电视剧", entries.single().group)
    }

    @Test
    fun detectsSingleStreamHlsManifest() {
        val manifest = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-TARGETDURATION:10
            #EXTINF:10.0,
            segment-1.ts
        """.trimIndent()

        assertTrue(parser.isHlsManifest(manifest))
        assertFalse(
            parser.isHlsManifest(
                """
                #EXTM3U
                #EXTINF:-1,频道
                https://example.com/a.m3u8
                """.trimIndent(),
            ),
        )
    }
}
