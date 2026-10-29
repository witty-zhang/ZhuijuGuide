package com.lzzhang.zhuijuguide

import com.lzzhang.zhuijuguide.data.ResourceJsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StarterCatalogTest {
    @Test
    fun bundledCatalogContainsUniqueHttpResources() {
        val candidates = listOf(
            File("src/main/assets/starter_resources.json"),
            File("app/src/main/assets/starter_resources.json"),
        )
        val file = candidates.firstOrNull(File::isFile)
            ?: error("Starter resource catalog not found")
        val resources = ResourceJsonParser().parse(file.readText())

        assertEquals(40, resources.size)
        assertEquals(resources.size, resources.map { it.id }.distinct().size)
        assertTrue(resources.all { it.url.startsWith("http://") || it.url.startsWith("https://") })
        assertTrue(resources.all { it.category.isNotBlank() })
        assertTrue(resources.any { it.title == "央视网直播" })
        assertTrue(resources.any { it.title == "Awesome Zhuiju Free" })
    }
}
