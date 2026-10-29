package com.lzzhang.zhuijuguide

import com.lzzhang.zhuijuguide.data.UrlNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class UrlNormalizerTest {
    @Test
    fun convertsChineseHostToPunycode() {
        assertEquals("http://xn--z7x900a.net/tv", UrlNormalizer.toAscii("http://肥猫.net/tv"))
        assertEquals(
            "http://www.xn--sss604efuw.cc/tv",
            UrlNormalizer.toAscii("http://www.饭太硬.cc/tv"),
        )
    }

    @Test
    fun keepsAsciiUrlsUntouched() {
        val url = "https://pan.vma.cc/down.php/abc.txt?x=1"
        assertEquals(url, UrlNormalizer.toAscii(url))
    }

    @Test
    fun keepsPortWhenConverting() {
        assertEquals("http://xn--z7x900a.net:8080/tv", UrlNormalizer.toAscii("http://肥猫.net:8080/tv"))
    }
}
