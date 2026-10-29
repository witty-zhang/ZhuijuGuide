package com.lzzhang.zhuijuguide

import com.lzzhang.zhuijuguide.data.LivePlaylistParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class LivePlaylistParserTest {
    private val parser = LivePlaylistParser()

    @Test
    fun parsesTxtListWithGenreGroups() {
        val entries = parser.parse(
            """
            网友提供频道
            世界地理,https://media.example/geo.m3u8
            央视,#genre#
            CCTV-1 综合,https://media.example/cctv1.m3u8
            CCTV-2 财经,https://media.example/cctv2.m3u8
            地方卫视,#genre#
            湖南卫视,https://media.example/hunan.m3u8
            """.trimIndent(),
        )

        assertEquals(4, entries.size)
        assertEquals("世界地理", entries[0].title)
        assertEquals(null, entries[0].group)
        assertEquals("央视", entries[1].group)
        assertEquals("https://media.example/cctv2.m3u8", entries[2].url)
        assertEquals("地方卫视", entries[3].group)
    }

    @Test
    fun skipsBrokenAndNonHttpLines() {
        val entries = parser.parse(
            """
            异形浩劫1971下（美国恐
            怖片）,https://media.example/movie.m3u8
            没有逗号的一行
            本地文件,/sdcard/video.m3u8
            广告,http://media.example/ad.m3u8,http://media.example/real.m3u8
            """.trimIndent(),
        )

        assertEquals(2, entries.size)
        assertEquals("https://media.example/movie.m3u8", entries[0].url)
        // 名称里带逗号时，地址取最后一个逗号之后的部分
        assertEquals("广告,http://media.example/ad.m3u8", entries[1].title)
        assertEquals("http://media.example/real.m3u8", entries[1].url)
    }

    @Test
    fun parsesM3uContentWithSameEntryPoint() {
        val entries = parser.parse(
            """
            #EXTM3U
            #EXTINF:-1 tvg-name="CCTV-1" group-title="央视",CCTV-1 综合
            https://media.example/cctv1.m3u8
            """.trimIndent(),
        )

        assertEquals(1, entries.size)
        assertEquals("CCTV-1", entries.single().title)
        assertEquals("央视", entries.single().group)
    }

    @Test
    fun decodesUtf8AndGbkFiles() {
        val text = "频道一,https://media.example/a.m3u8\n频道二,https://media.example/b.m3u8"

        assertEquals(text, parser.decode(text.toByteArray(Charsets.UTF_8)))
        assertEquals(text, parser.decode(text.toByteArray(Charset.forName("GBK"))))
    }

    @Test
    fun dropsDuplicateUrls() {
        val entries = parser.parse(
            """
            央视,#genre#
            CCTV-1,https://media.example/cctv1.m3u8
            CCTV-1 备用,https://media.example/cctv1.m3u8
            """.trimIndent(),
        )

        assertEquals(1, entries.size)
        assertTrue(entries.single().title == "CCTV-1")
    }
}
