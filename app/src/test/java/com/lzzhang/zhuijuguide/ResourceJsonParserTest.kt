package com.lzzhang.zhuijuguide

import com.lzzhang.zhuijuguide.data.ResourceJsonParser
import com.lzzhang.zhuijuguide.model.Availability
import com.lzzhang.zhuijuguide.model.ResourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceJsonParserTest {
    private val parser = ResourceJsonParser()

    @Test
    fun parsesWrappedResourcesAndNormalizesFields() {
        val resources = parser.parse(
            """
            {
              "data": {
                "items": [
                  {
                    "name": "示例直播",
                    "link": "https://media.example/live/index.m3u8",
                    "summary": "测试资源",
                    "category": "直播",
                    "state": "可访问",
                    "risk_level": "中",
                    "score": 4.5
                  }
                ]
              }
            }
            """.trimIndent(),
        )

        assertEquals(1, resources.size)
        val resource = resources.single()
        assertEquals("示例直播", resource.title)
        assertEquals(ResourceType.VIDEO, resource.type)
        assertEquals(Availability.AVAILABLE, resource.availability)
        assertEquals(4.5f, resource.rating)
    }

    @Test
    fun skipsItemsWithoutTitleOrUrl() {
        val resources = parser.parse(
            """
            [
              {"title": "没有地址"},
              {"url": "https://example.com"},
              {"title": "有效", "url": "https://example.com"}
            ]
            """.trimIndent(),
        )

        assertEquals(1, resources.size)
        assertTrue(resources.single().id.isNotBlank())
    }
}
