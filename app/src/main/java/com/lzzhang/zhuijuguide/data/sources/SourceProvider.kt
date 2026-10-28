package com.lzzhang.zhuijuguide.data.sources

import com.lzzhang.zhuijuguide.model.Episode
import com.lzzhang.zhuijuguide.model.WorkSummary
import java.util.concurrent.ConcurrentHashMap

/**
 * 数据源适配接口。新增一个来源只需要实现它并注册到 [SourceRegistry]，
 * 首页推荐、搜索、详情、播放都不需要改。
 */
interface SourceProvider {
    val id: String
    val name: String
    val tagline: String

    /** 首页推荐用，返回该来源的精选内容。 */
    suspend fun featured(): List<WorkSummary>

    /** 跨来源统一搜索。 */
    suspend fun search(query: String): List<WorkSummary>

    /** 取某部作品可播放的地址列表（剧集、或不同清晰度）。 */
    suspend fun episodes(work: WorkSummary): List<Episode>
}

class SourceRegistry(val providers: List<SourceProvider>) {
    private val seenWorks = ConcurrentHashMap<String, WorkSummary>()

    fun remember(works: List<WorkSummary>) {
        works.forEach { seenWorks[it.id] = it }
    }

    fun workById(id: String): WorkSummary? = seenWorks[id]
}
