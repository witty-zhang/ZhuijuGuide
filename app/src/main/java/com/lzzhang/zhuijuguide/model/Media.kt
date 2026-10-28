package com.lzzhang.zhuijuguide.model

import kotlinx.serialization.Serializable

/** 一部影片/一期节目，跨数据源统一表示。 */
@Serializable
data class WorkSummary(
    val id: String,
    val sourceId: String,
    val sourceName: String = "",
    val itemId: String,
    val title: String,
    val description: String,
    val posterUrl: String? = null,
    val dateText: String? = null,
)

/** 可播放的一集/一个清晰度，url 是播放器能直接打开的媒体地址。 */
@Serializable
data class Episode(
    val title: String,
    val url: String,
)

data class WorkDetail(
    val work: WorkSummary,
    val episodes: List<Episode>,
)
