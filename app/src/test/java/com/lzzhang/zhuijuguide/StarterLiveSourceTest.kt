package com.lzzhang.zhuijuguide

import com.lzzhang.zhuijuguide.data.ResourceJsonParser
import com.lzzhang.zhuijuguide.model.ResourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StarterLiveSourceTest {
    @Test
    fun bundledLiveSourcesAreListedWithExpectedTypes() {
        val candidates = listOf(
            File("src/main/assets/starter_live_sources.json"),
            File("app/src/main/assets/starter_live_sources.json"),
        )
        val file = candidates.firstOrNull(File::isFile)
            ?: error("Starter live source catalog not found")
        val sources = ResourceJsonParser().parse(file.readText())

        assertEquals(3, sources.size)
        assertEquals(sources.size, sources.map { it.id }.distinct().size)
        assertTrue(sources.all { it.category == "直播源" })

        val vma = sources.single { it.title == "VMA 直播源" }
        // .txt 清单要按播放列表处理，点开才会去下载频道
        assertEquals(ResourceType.PLAYLIST, vma.type)

        val fantaiying = sources.single { it.title == "饭太硬" }
        assertEquals("http://www.饭太硬.cc/tv", fantaiying.url)
        assertEquals(ResourceType.WEB, fantaiying.type)

        val feimao = sources.single { it.title == "肥猫" }
        assertEquals("http://肥猫.net/tv", feimao.url)
    }
}
